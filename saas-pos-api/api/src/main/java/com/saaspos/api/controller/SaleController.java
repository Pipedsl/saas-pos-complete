package com.saaspos.api.controller;

import com.saaspos.api.dto.SaleRequest;
// Importamos la clase interna correctamente o la referenciamos directamente
import com.saaspos.api.model.*;
import com.saaspos.api.repository.*;
import com.saaspos.api.service.SaleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private final SaleService saleService;
    private final UserRepository userRepository;

    // Repositorios para la edición directa
    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final InventoryLogRepository inventoryLogRepository;

    public SaleController(SaleService saleService,
                          UserRepository userRepository,
                          SaleRepository saleRepository,
                          ProductRepository productRepository,
                          InventoryLogRepository inventoryLogRepository) {
        this.saleService = saleService;
        this.userRepository = userRepository;
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.inventoryLogRepository = inventoryLogRepository;
    }

    // 1. Obtener Historial de Ventas (Con filtro de fecha opcional)
    @GetMapping
    public ResponseEntity<List<Sale>> getMySales(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {

        UUID tenantId = getCurrentTenantId();

        // Si no mandan fechas, traemos las de hoy por defecto para no cargar millones de registros
        // O podrías implementar paginación, pero por ahora usemos un rango seguro.
        if (start == null || end == null) {
            // Lógica simple: Traer todo (o podrías limitar a los últimos 30 días en el repo)
            return ResponseEntity.ok(saleRepository.findByTenantIdOrderByCreatedAtDesc(tenantId));
        }

        // Aquí podrías implementar la búsqueda por rango si agregas el método al repositorio
        // Por simplicidad ahora, devolvemos todo ordenado por fecha
        return ResponseEntity.ok(saleRepository.findByTenantIdOrderByCreatedAtDesc(tenantId));
    }

    // 2. Obtener Detalle de una Venta
    @GetMapping("/{id}")
    @Transactional(readOnly = true) // Buena práctica para lectura
    public ResponseEntity<Sale> getSaleById(@PathVariable UUID id) {
        UUID tenantId = getCurrentTenantId();

        // CAMBIO AQUÍ: Usamos findByIdWithItems
        Sale sale = saleRepository.findByIdWithItems(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));

        if (!sale.getTenantId().equals(tenantId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(sale);
    }

    @PostMapping
    public ResponseEntity<?> createSale(@RequestBody SaleRequest request) {
        try {
            UUID tenantId = getCurrentTenantId();
            Sale sale = saleService.processSale(request, tenantId);
            Sale fullSale = saleRepository.findByIdWithItems(sale.getId()).orElse(sale);
            return ResponseEntity.ok(fullSale);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- ENDPOINT: EDITAR VENTA ---
    @PutMapping("/{saleId}")
    @Transactional
    public ResponseEntity<?> updateSale(@PathVariable UUID saleId, @RequestBody SaleRequest request) {
        UUID tenantId = getCurrentTenantId();
        User currentUser = getCurrentUser();

        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));

        // 1. Validar Tenant
        if (!sale.getTenantId().equals(tenantId)) {
            return ResponseEntity.status(403).body("No autorizado");
        }

        // 2. REVERSA: Devolver Stock Antiguo
        for (SaleItem oldItem : sale.getItems()) {
            Product p = oldItem.getProduct();
            if (p != null) {
                BigDecimal oldStock = p.getStockCurrent();
                BigDecimal qtyToReturn = oldItem.getQuantity();

                // Devolver stock
                p.setStockCurrent(oldStock.add(qtyToReturn));
                productRepository.save(p);

                // Auditoría
                createLog(p, currentUser, "SALE_EDIT_RETURN", qtyToReturn, oldStock, p.getStockCurrent(),
                        "Devolución por edición de Ticket #" + sale.getSaleNumber(), sale.getId(), null);
            }
        }

        // Limpiar items antiguos (orphanRemoval=true en entidad Sale se encarga del delete)
        sale.getItems().clear();

        // 3. NUEVA VENTA: Descontar Stock Nuevo
        BigDecimal newTotal = BigDecimal.ZERO;

        // AQUÍ ESTABA EL ERROR: Referenciamos la clase interna correctamente
        for (SaleRequest.SaleItemRequest itemReq : request.getItems()) {
            Product p = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + itemReq.getProductId()));

            BigDecimal qty = itemReq.getQuantity();
            BigDecimal oldStock = p.getStockCurrent();

            // Validar Stock
            if (p.getStockCurrent().compareTo(qty) < 0) {
                throw new RuntimeException("Stock insuficiente para: " + p.getName());
            }

            // Descontar
            p.setStockCurrent(oldStock.subtract(qty));
            productRepository.save(p);

            // Auditoría (Salida)
            createLog(p, currentUser, "SALE_EDIT_OUT", qty.negate(), oldStock, p.getStockCurrent(),
                    "Re-emisión de Ticket #" + sale.getSaleNumber(), sale.getId(), null);

            // Crear Item
            SaleItem newItem = new SaleItem();
            newItem.setSale(sale);
            newItem.setProduct(p);
            newItem.setQuantity(qty);

            // Usamos precio actual del producto o el que viene en el request si lo permites
            // Por seguridad usamos el del producto, a menos que quieras permitir precio manual
            newItem.setUnitPrice(p.getPriceFinal());
            newItem.setTotal(newItem.getUnitPrice().multiply(qty));

            // --- FIX: LLENAR CAMPOS DE COSTO E IMPUESTOS OBLIGATORIOS ---
            // 1. Costo (Si es null, usamos 0 para evitar el error de BD)
            newItem.setCostPriceAtSale(p.getCostPrice() != null ? p.getCostPrice() : BigDecimal.ZERO);

            // 2. Precio Neto (Base imponible)
            // Si por alguna razón el producto no tiene neto calculado, usamos el precio final como fallback
            BigDecimal netPrice = p.getPriceNeto() != null ? p.getPriceNeto() : p.getPriceFinal();
            newItem.setNetPriceAtSale(netPrice);

            // 3. Impuestos
            // Impuesto Unitario = Precio Final - Neto
            BigDecimal unitTax = p.getPriceFinal().subtract(netPrice);
            newItem.setUnitTax(unitTax);

            // Impuesto Total de la línea = Impuesto Unitario * Cantidad
            newItem.setTaxAmountAtSale(unitTax.multiply(qty));
            // -------------------------------------------------------------

            sale.getItems().add(newItem);
            newTotal = newTotal.add(newItem.getTotal());
        }

        // 4. Actualizar Cabecera
        sale.setTotalAmount(newTotal);
        sale.setWasEdited(true);
        sale.setEditedByUserId(currentUser.getId());

        // Usamos el nuevo campo 'notes' del request
        String reason = (request.getNotes() != null && !request.getNotes().isEmpty())
                ? request.getNotes()
                : "Edición manual de ticket";
        sale.setEditReason(reason);

        sale.setStatus("COMPLETED");

        saleRepository.save(sale);
        return ResponseEntity.ok(sale);
    }

    // --- MÉTODOS AUXILIARES ---

    private void createLog(Product p, User u, String action, BigDecimal change, BigDecimal oldStk, BigDecimal newStk, String reason, UUID saleId, UUID webOrderId) {
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
        log.setWebOrderId(webOrderId);
        inventoryLogRepository.save(log);
    }

    private UUID getCurrentTenantId() {
        return getCurrentUser().getTenant().getId();
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }
}