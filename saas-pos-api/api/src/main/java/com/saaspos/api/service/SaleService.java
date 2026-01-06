package com.saaspos.api.service;

import com.saaspos.api.dto.SaleRequest;
import com.saaspos.api.model.*;
import com.saaspos.api.repository.InventoryLogRepository;
import com.saaspos.api.repository.ProductRepository;
import com.saaspos.api.repository.SaleRepository;
import com.saaspos.api.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final UserRepository userRepository;

    public SaleService(SaleRepository saleRepository,
                       ProductRepository productRepository,
                       InventoryLogRepository inventoryLogRepository,
                       UserRepository userRepository) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.inventoryLogRepository = inventoryLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Sale processSale(SaleRequest request, UUID tenantId) {
        Sale sale = new Sale();
        // ELIMINADO: sale.setId(...) -> Dejamos que Hibernate/DB genere el UUID
        sale.setTenantId(tenantId);

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(email).orElseThrow();

        // 1. Configurar Cabecera
        BigDecimal total = request.getTotalAmount();
        sale.setTotalAmount(total);

        // Cálculos aproximados de cabecera
        BigDecimal subtotal = total.divide(new BigDecimal("1.19"), 0, RoundingMode.HALF_UP);
        BigDecimal tax = total.subtract(subtotal);
        sale.setSubtotalAmount(subtotal);
        sale.setTotalTax(tax);

        sale.setSaleNumber("TCK-" + System.currentTimeMillis());
        sale.setStatus("COMPLETED");

        // --- CORRECCIÓN CRÍTICA ---
        // Guardamos la venta "vacía" primero. Esto hace el INSERT en la BD.
        // Hibernate genera el ID y lo asigna al objeto 'sale'.
        sale = saleRepository.save(sale);
        // --------------------------

        // 2. Procesar Ítems (Ahora sale.getId() es válido y existe en BD)
        for (SaleRequest.SaleItemRequest itemDto : request.getItems()) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new RuntimeException("Producto no existe"));

            if (!product.getTenantId().equals(tenantId)) {
                throw new RuntimeException("Error de seguridad");
            }

            BigDecimal qtyToSell = itemDto.getQuantity() != null ? itemDto.getQuantity() : BigDecimal.ZERO;

            // --- LÓGICA DE STOCK (Packs vs Normal) ---
            if ("BUNDLE".equals(product.getProductType())) {
                BigDecimal packStock = product.getStockCurrent();

                // Caso 1: Pack Limitado
                if (packStock != null) {
                    if (packStock.compareTo(qtyToSell) < 0) throw new RuntimeException("Pack Agotado");
                    product.setStockCurrent(packStock.subtract(qtyToSell));
                    productRepository.save(product);
                }

                // Caso 2: Pack Ilimitado (NULL) -> Solo descontamos hijos
                for (BundleItem bundleItem : product.getBundleItems()) {
                    Product child = bundleItem.getComponentProduct();
                    BigDecimal totalChildQty = bundleItem.getQuantity().multiply(qtyToSell);

                    if (child.getStockCurrent().compareTo(totalChildQty) < 0) throw new RuntimeException("Sin stock componente: " + child.getName());

                    BigDecimal oldStock = child.getStockCurrent();
                    child.setStockCurrent(oldStock.subtract(totalChildQty));
                    productRepository.save(child);

                    // Log del componente hijo (Usando el ID generado)
                    createLog(child, currentUser, "BUNDLE_SALE", totalChildQty.negate(), oldStock, child.getStockCurrent(),
                            "Venta Pack POS: " + product.getName(), sale.getId());
                }
            } else {
                // Caso 3: Producto Normal
                BigDecimal currentStock = product.getStockCurrent();
                if (currentStock.compareTo(qtyToSell) < 0) throw new RuntimeException("Sin stock: " + product.getName());

                BigDecimal oldStock = currentStock; // Guardamos stock antes de restar
                product.setStockCurrent(currentStock.subtract(qtyToSell));
                productRepository.save(product);

                // Log de venta normal (Usando el ID generado)
                createLog(product, currentUser, "SALE", qtyToSell.negate(), oldStock, product.getStockCurrent(),
                        "Venta POS", sale.getId());
            }

            // --- LÓGICA DE PRECIOS FLEXIBLES ---
            SaleItem saleItem = new SaleItem();
            saleItem.setProduct(product);
            saleItem.setQuantity(qtyToSell);

            BigDecimal finalUnitPrice;
            boolean priceChanged = false;

            if (itemDto.getCustomPrice() != null && itemDto.getCustomPrice().compareTo(BigDecimal.ZERO) >= 0) {
                finalUnitPrice = itemDto.getCustomPrice();
                priceChanged = true;
            } else {
                finalUnitPrice = product.getPriceFinal();
            }

            // Guardar Costos e Impuestos
            BigDecimal costAtMoment = product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO;
            saleItem.setCostPriceAtSale(costAtMoment);

            // Cálculo inverso de impuestos (asumiendo precio BRUTO)
            BigDecimal taxPercent = product.getTaxPercent() != null ? product.getTaxPercent() : new BigDecimal("19.0");
            BigDecimal taxFactor = taxPercent.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            BigDecimal divisor = BigDecimal.ONE.add(taxFactor);

            BigDecimal netPrice;
            BigDecimal unitTaxCalc;

            if (product.getIsTaxIncluded()) {
                netPrice = finalUnitPrice.divide(divisor, 4, RoundingMode.HALF_UP);
                unitTaxCalc = finalUnitPrice.subtract(netPrice);
            } else {
                netPrice = finalUnitPrice.divide(divisor, 4, RoundingMode.HALF_UP); // Ajuste estándar POS
                unitTaxCalc = finalUnitPrice.subtract(netPrice);
            }

            saleItem.setNetPriceAtSale(netPrice);
            saleItem.setUnitTax(unitTaxCalc);
            saleItem.setTaxAmountAtSale(unitTaxCalc.multiply(qtyToSell));
            saleItem.setUnitPrice(finalUnitPrice);
            saleItem.setTotal(finalUnitPrice.multiply(qtyToSell));

            // Agregamos a la lista de la entidad (Cascade se encargará de guardar esto al final)
            sale.addItem(saleItem);

            // Log de Cambio de Precio (Usando el ID generado)
            if (priceChanged) {
                String logMsg = String.format("Precio modificado en caja: $%s -> $%s", product.getPriceFinal(), finalUnitPrice);
                createLog(product, currentUser, "PRICE_OVERRIDE", BigDecimal.ZERO, product.getStockCurrent(), product.getStockCurrent(),
                        logMsg, sale.getId());
            }
        }

        // 3. GUARDADO FINAL (UPDATE)
        // Esto guarda los SaleItems gracias a CascadeType.ALL en la entidad Sale
        return saleRepository.save(sale);
    }

    private void createLog(Product p, User u, String action, BigDecimal change, BigDecimal oldStk, BigDecimal newStk, String reason, UUID saleId) {
        InventoryLog log = new InventoryLog();
        log.setTenantId(p.getTenantId());
        log.setProductId(p.getId());
        log.setProductNameSnapshot(p.getName());
        log.setUserId(u.getId());
        log.setUserNameSnapshot(u.getFullName());
        log.setActionType(action);
        log.setQuantityChange(change);
        log.setOldStock(oldStk);
        log.setNewStock(newStk);
        log.setReason(reason);
        log.setSaleId(saleId);
        inventoryLogRepository.save(log);
    }
}