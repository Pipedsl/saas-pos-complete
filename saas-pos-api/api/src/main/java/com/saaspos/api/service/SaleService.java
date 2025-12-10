package com.saaspos.api.service;

import com.saaspos.api.dto.SaleRequest;
import com.saaspos.api.model.Product;
import com.saaspos.api.model.Sale;
import com.saaspos.api.model.SaleItem;
import com.saaspos.api.repository.ProductRepository;
import com.saaspos.api.repository.SaleRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode; // Importante para división de moneda
import java.util.UUID;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;

    public SaleService(SaleRepository saleRepository, ProductRepository productRepository) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public Sale processSale(SaleRequest request, UUID tenantId) {
        Sale sale = new Sale();
        sale.setTenantId(tenantId);

        // 1. Obtener el Total Bruto (Con IVA)
        BigDecimal total = request.getTotalAmount();
        sale.setTotalAmount(total);

        // 2. CÁLCULO DE IMPUESTOS (ESTO FALTABA)
        // Asumiendo IVA 19% (Factor 1.19).
        // Fórmula: Neto = Total / 1.19
        // RoundingMode.HALF_UP es el redondeo estándar matemático.
        BigDecimal subtotal = total.divide(new BigDecimal("1.19"), 0, RoundingMode.HALF_UP);
        BigDecimal tax = total.subtract(subtotal);

        // 3. Llenar los campos obligatorios de la BD
        sale.setSubtotalAmount(subtotal); // <--- ESTO ARREGLA EL ERROR "null value"
        sale.setTotalTax(tax);

        // 4. Datos administrativos
        sale.setSaleNumber("TCK-" + System.currentTimeMillis());
        sale.setStatus("COMPLETED");

        // 5. Procesar Ítems y Descontar Stock
        for (SaleRequest.SaleItemRequest itemDto : request.getItems()) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new RuntimeException("Producto ID " + itemDto.getProductId() + " no existe"));

            // Validar Tenant (Seguridad)
            if (!product.getTenantId().equals(tenantId)) {
                throw new RuntimeException("Error de seguridad: Producto de otro tenant");
            }

            // Obtener valores BigDecimal seguros
            BigDecimal currentStock = product.getStockCurrent() != null ? product.getStockCurrent() : BigDecimal.ZERO;
            BigDecimal qtyToSell = itemDto.getQuantity() != null ? itemDto.getQuantity() : BigDecimal.ZERO;

            // Validar Stock
            if (currentStock.compareTo(qtyToSell) < 0) {
                throw new RuntimeException("Stock insuficiente para: " + product.getName() + ". Disponible: " + currentStock);
            }

            // Descontar Stock
            product.setStockCurrent(currentStock.subtract(qtyToSell));
            productRepository.save(product);

            // Guardar Detalle (SaleItem)
            SaleItem saleItem = new SaleItem();
            saleItem.setProduct(product);
            saleItem.setQuantity(qtyToSell);
            // 1. Guardar Costo Histórico (Snapshot)
            BigDecimal costAtMoment = product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO;
            saleItem.setCostPriceAtSale(costAtMoment);

            // 2. Guardar Precio Neto Histórico (Base imponible)
            BigDecimal netPrice = product.getPriceNeto();
            saleItem.setNetPriceAtSale(netPrice); // <--- NUEVO CAMPO EN ENTIDAD SaleItem

            // 3. Guardar Impuesto (Para tema legal)
            BigDecimal taxPercent = product.getTaxPercent() != null ? product.getTaxPercent() : new BigDecimal("19.0");
            BigDecimal taxFactor = taxPercent.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

            // Impuesto unitario: 100 * 0.19 = 19
            BigDecimal unitTaxCalc = netPrice.multiply(taxFactor);
            saleItem.setUnitTax(unitTaxCalc);

            // Impuesto total línea (opcional, si agregaste la columna tax_amount_at_sale)
            saleItem.setTaxAmountAtSale(unitTaxCalc.multiply(qtyToSell));

            // 4. Precio Unitario Final (Gross/Bruto) para el ticket
            // Neto (100) + Impuesto (19) = 119
            BigDecimal grossUnitPrice = netPrice.add(unitTaxCalc);

            // Redondeamos a 0 decimales para CLP (pesos chilenos)
            saleItem.setUnitPrice(grossUnitPrice.setScale(0, RoundingMode.HALF_UP));

            sale.addItem(saleItem);
        }

        return saleRepository.save(sale);
    }
}