package com.saaspos.api.controller;

import com.saaspos.api.dto.ecommerce.WebOrderItemRequest;
import com.saaspos.api.dto.ecommerce.WebOrderRequest;
import com.saaspos.api.model.BundleItem;
import com.saaspos.api.model.InventoryLog;
import com.saaspos.api.model.Product;
import com.saaspos.api.model.User;
import com.saaspos.api.model.ecommerce.WebOrder;
import com.saaspos.api.model.ecommerce.WebOrderItem;
import com.saaspos.api.repository.InventoryLogRepository; // <--- NUEVO
import com.saaspos.api.repository.ProductRepository;
import com.saaspos.api.repository.UserRepository;
import com.saaspos.api.repository.WebOrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/web-orders")
public class AdminWebOrderController {

    private final WebOrderRepository webOrderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final InventoryLogRepository inventoryLogRepository; // <--- INYECCIÓN

    public AdminWebOrderController(WebOrderRepository webOrderRepository,
                                   UserRepository userRepository,
                                   ProductRepository productRepository,
                                   InventoryLogRepository inventoryLogRepository) {
        this.webOrderRepository = webOrderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.inventoryLogRepository = inventoryLogRepository;
    }

    @GetMapping("/{orderNumber}")
    public ResponseEntity<WebOrder> getOrder(@PathVariable String orderNumber) {
        UUID tenantId = getCurrentTenantId();
        WebOrder order = webOrderRepository.findByOrderNumberAndTenantId(orderNumber, tenantId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        return ResponseEntity.ok(order);
    }

    // 2. Actualizar Estado con Lógica de Stock y Auditoría
    @PatchMapping("/{orderNumber}/status")
    @Transactional
    public ResponseEntity<?> updateStatus(@PathVariable String orderNumber, @RequestBody String newStatusRaw) {
        UUID tenantId = getCurrentTenantId();
        String newStatus = newStatusRaw.replace("\"", ""); // Limpiar comillas si vienen del JSON

        WebOrder order = webOrderRepository.findByOrderNumberAndTenantId(orderNumber, tenantId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        String oldStatus = order.getStatus();

        if (oldStatus.equals(newStatus)) {
            return ResponseEntity.ok(order);
        }

        User currentUser = getCurrentUserEntity(); // Quién hace el cambio

        boolean wasStockHeld = isStockHeld(oldStatus);
        boolean willStockBeHeld = isStockHeld(newStatus);

        try {
            // CASO 1: Liberación de Stock (Ej: PENDING -> CANCELLED)
            if (wasStockHeld && !willStockBeHeld) {
                // Devolver stock y registrar log
                modifyStock(order, true, currentUser, "Liberación por cambio de estado: " + oldStatus + " -> " + newStatus);
            }
            // CASO 2: Re-captura de Stock (Ej: CANCELLED -> PENDING - Reactivar pedido)
            else if (!wasStockHeld && willStockBeHeld) {
                // Restar stock y registrar log
                modifyStock(order, false, currentUser, "Reserva por cambio de estado: " + oldStatus + " -> " + newStatus);
            }

            order.setStatus(newStatus);
            if (newStatus.equals("CANCELLED")) {
                order.setEditReason("Cancelado manualmente por: " + currentUser.getFullName());
            }

            return ResponseEntity.ok(webOrderRepository.save(order));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{orderNumber}/items")
    @Transactional
    public ResponseEntity<WebOrder> updateOrderItems(@PathVariable String orderNumber, @RequestBody WebOrderRequest request) {
        UUID tenantId = getCurrentTenantId();
        User currentUser = getCurrentUserEntity();
        WebOrder order = webOrderRepository.findByOrderNumberAndTenantId(orderNumber, tenantId).orElseThrow();

        // 1. DEVOLVER STOCK DE ÍTEMS ANTIGUOS (Esto queda igual)
        for (WebOrderItem oldItem : order.getItems()) {
            Product p = oldItem.getProduct();
            if (p != null) {
                if ("BUNDLE".equals(p.getProductType())) {
                    for (BundleItem childItem : p.getBundleItems()) {
                        Product child = childItem.getComponentProduct();
                        BigDecimal totalQty = childItem.getQuantity().multiply(oldItem.getQuantity());
                        BigDecimal oldStk = child.getStockCurrent();
                        child.setStockCurrent(oldStk.add(totalQty));
                        productRepository.save(child);
                        // Log simple de retorno
                        createInventoryLog(child, currentUser, "WEB_ORDER_EDIT_RETURN", totalQty, oldStk, child.getStockCurrent(), "Edición Web (Re-cálculo)", order.getId());
                    }
                } else {
                    BigDecimal oldStk = p.getStockCurrent();
                    p.setStockCurrent(oldStk.add(oldItem.getQuantity()));
                    productRepository.save(p);
                    // Log simple de retorno
                    createInventoryLog(p, currentUser, "WEB_ORDER_EDIT_RETURN", oldItem.getQuantity(), oldStk, p.getStockCurrent(), "Edición Web (Re-cálculo)", order.getId());
                }
            }
        }

        order.getItems().clear();

        // 2. PROCESAR NUEVOS ÍTEMS Y DESCONTAR STOCK (Aquí mejoramos el Log)
        BigDecimal newTotal = BigDecimal.ZERO;

        for (WebOrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId()).orElseThrow();
            BigDecimal qty = new BigDecimal(itemReq.getQuantity());

            // --- LÓGICA DE DETECCIÓN DE CAMBIOS Y PIN ---
            BigDecimal priceToUse;
            String priceLogDetail = "";

            // Determinar si hubo cambio de precio
            if (itemReq.getCustomPrice() != null) {
                priceToUse = itemReq.getCustomPrice();
                // Si el precio difiere del original, lo anotamos
                if (priceToUse.compareTo(product.getPriceFinal()) != 0) {
                    priceLogDetail = String.format(" [Precio: $%s -> $%s]", product.getPriceFinal(), priceToUse);
                }
            } else {
                priceToUse = product.getPriceFinal();
            }

            // Determinar si se usó PIN (Si es Cajero, asumimos que sí porque el front lo obligó)
            String authLog = currentUser.getRole().equals("CASHIER") ? " (Autorizado PIN)" : "";

            // Construir el mensaje final del Log
            String finalLogReason = "Edición Web" + priceLogDetail + authLog;
            // ---------------------------------------------

            if ("BUNDLE".equals(product.getProductType())) {
                for (BundleItem childItem : product.getBundleItems()) {
                    Product child = childItem.getComponentProduct();
                    BigDecimal totalQty = childItem.getQuantity().multiply(qty);

                    if (child.getStockCurrent().compareTo(totalQty) < 0) throw new RuntimeException("Sin stock componente: " + child.getName());

                    BigDecimal oldStk = child.getStockCurrent();
                    child.setStockCurrent(oldStk.subtract(totalQty));
                    productRepository.save(child);
                    // Usamos el mensaje detallado
                    createInventoryLog(child, currentUser, "WEB_ORDER_EDIT_OUT", totalQty.negate(), oldStk, child.getStockCurrent(), finalLogReason, order.getId());
                }
            } else {
                if (product.getStockCurrent().compareTo(qty) < 0) throw new RuntimeException("Sin stock: " + product.getName());

                BigDecimal oldStk = product.getStockCurrent();
                product.setStockCurrent(oldStk.subtract(qty));
                productRepository.save(product);
                // Usamos el mensaje detallado
                createInventoryLog(product, currentUser, "WEB_ORDER_EDIT_OUT", qty.negate(), oldStk, product.getStockCurrent(), finalLogReason, order.getId());
            }

            // Crear el Item en la Orden
            WebOrderItem newItem = new WebOrderItem();
            newItem.setWebOrder(order);
            newItem.setProduct(product);
            newItem.setQuantity(qty);
            newItem.setUnitPriceAtMoment(priceToUse); // Usamos el precio calculado
            newItem.setCostPriceAtMoment(product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO);
            newItem.setProductNameSnapshot(product.getName());
            newItem.setSkuSnapshot(product.getSku());

            BigDecimal subtotal = priceToUse.multiply(qty);
            newItem.setSubtotal(subtotal);
            order.getItems().add(newItem);
            newTotal = newTotal.add(subtotal);
        }

        order.setFinalTotal(newTotal.add(order.getShippingCost() != null ? order.getShippingCost() : BigDecimal.ZERO));
        order.setTotalItems(new BigDecimal(order.getItems().size()));
        order.setWasEdited(true);
        order.setEditedByUserId(currentUser.getId());

        // Mensaje global en la orden
        String roleMsg = currentUser.getRole().equals("CASHIER") ? " (Con PIN)" : "";
        order.setEditReason("Editado por: " + currentUser.getFullName() + roleMsg);

        return ResponseEntity.ok(webOrderRepository.save(order));
    }

    // --- MÉTODOS AUXILIARES ---

    private boolean isStockHeld(String status) {
        return status.equals("PENDING") || status.equals("PAID") || status.equals("CONFIRMED") ||
                status.equals("PREPARING") || status.equals("SHIPPED") || status.equals("DELIVERED");
    }

    private void modifyStock(WebOrder order, boolean isReturn, User user, String reason) {
        for (WebOrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (product != null) {
                // Recargamos producto para tener datos frescos (y sus bundleItems)
                Product dbProduct = productRepository.findById(product.getId()).orElse(product);
                BigDecimal qtyOrder = item.getQuantity();

                if ("BUNDLE".equals(dbProduct.getProductType())) {
                    // SI ES PACK -> Afectamos a los HIJOS
                    for (BundleItem bundleItem : dbProduct.getBundleItems()) {
                        Product child = bundleItem.getComponentProduct();
                        BigDecimal qtyChildTotal = bundleItem.getQuantity().multiply(qtyOrder);

                        applyStockChange(child, isReturn, qtyChildTotal, user, reason + " (Pack: " + dbProduct.getName() + ")", order.getId());
                    }
                } else {
                    // SI ES NORMAL -> Afectamos al PRODUCTO
                    applyStockChange(dbProduct, isReturn, qtyOrder, user, reason, order.getId());
                }
            }
        }
    }

    // Método refactorizado para aplicar el cambio a un producto individual (sea hijo o normal)
    private void applyStockChange(Product p, boolean isReturn, BigDecimal quantity, User user, String reason, UUID orderId) {
        BigDecimal oldStock = p.getStockCurrent();
        BigDecimal newStock;
        String action;
        BigDecimal change;

        if (isReturn) {
            newStock = oldStock.add(quantity);
            change = quantity;
            action = "WEB_ORDER_RETURN";
        } else {
            if (oldStock.compareTo(quantity) < 0) {
                throw new RuntimeException("No hay stock suficiente: " + p.getName());
            }
            newStock = oldStock.subtract(quantity);
            change = quantity.negate();
            action = "WEB_ORDER_REACTIVATE";
        }

        p.setStockCurrent(newStock);
        productRepository.save(p);
        createInventoryLog(p, user, action, change, oldStock, newStock, reason, orderId);
    }



    private void createInventoryLog(Product p, User u, String action, BigDecimal change, BigDecimal oldStk, BigDecimal newStk, String reason, UUID webOrderId) {
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
        log.setWebOrderId(webOrderId);
        inventoryLogRepository.save(log);
    }

    private UUID getCurrentTenantId() {
        return getCurrentUserEntity().getTenant().getId();
    }

    private User getCurrentUserEntity() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }
}