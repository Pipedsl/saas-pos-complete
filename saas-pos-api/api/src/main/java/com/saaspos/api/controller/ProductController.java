package com.saaspos.api.controller;

import com.saaspos.api.dto.ProductDto;
import com.saaspos.api.model.*;
import com.saaspos.api.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final SaleItemRepository saleItemRepository;
    private final WebOrderRepository webOrderRepository;
    private final WebOrderItemRepository webOrderItemRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final ProductPriceHistoryRepository productPriceHistoryRepository;
    // NUEVO REPOSITORIO
    private final BundleItemRepository bundleItemRepository;

    public ProductController(ProductRepository productRepository, UserRepository userRepository, TenantRepository tenantRepository, CategoryRepository categoryRepository, SupplierRepository supplierRepository, SaleItemRepository saleItemRepository, WebOrderRepository webOrderRepository, WebOrderItemRepository webOrderItemRepository, ProductPriceHistoryRepository productPriceHistoryRepository, InventoryLogRepository inventoryLogRepository, BundleItemRepository bundleItemRepository) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
        this.saleItemRepository = saleItemRepository;
        this.webOrderRepository = webOrderRepository;
        this.webOrderItemRepository = webOrderItemRepository;
        this.productPriceHistoryRepository = productPriceHistoryRepository;
        this.inventoryLogRepository = inventoryLogRepository;
        this.bundleItemRepository = bundleItemRepository;
    }

    // ... (GET getAllProducts IGUAL) ...
    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        UUID tenantId = getCurrentTenantId();
        List<Product> products = productRepository.findByTenantId(tenantId);
        List<ProductDto> dtos = products.stream().map(this::mapToDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // POST: Crear nuevo producto (Con validación de Stock Pack)
    @PostMapping
    @Transactional
    public ResponseEntity<?> createProduct(@RequestBody ProductDto dto) {
        UUID tenantId = getCurrentTenantId();
        User currentUser = getCurrentUserEntity();

        // 1. Validaciones básicas (Plan y SKU) - IGUAL QUE ANTES
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow(() -> new RuntimeException("Tenant no existe"));
        Plan plan = tenant.getPlan();
        long currentProducts = productRepository.countByTenantId(tenantId);
        if (plan != null && currentProducts >= plan.getMaxProducts()) {
            return ResponseEntity.status(403).body("Límite de productos alcanzado.");
        }
        if (productRepository.existsByTenantIdAndSku(tenantId, dto.getSku())) {
            return ResponseEntity.badRequest().body("El SKU ya existe.");
        }

        Product product = new Product();
        product.setTenantId(tenantId);
        product.setSku(dto.getSku());
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        calculateProductPrices(product, dto);

        product.setStockMin(dto.getStockMin() != null ? dto.getStockMin() : new BigDecimal("5"));
        product.setAttributes(dto.getAttributes());
        product.setMeasurementUnit(dto.getMeasurementUnit() != null ? dto.getMeasurementUnit() : "UNIT");
        product.setImageUrl(dto.getImageUrl());
        product.setIsPublic(dto.getIsPublic() != null ? dto.getIsPublic() : false);
        product.setActive(true);
        product.setProductType(dto.getProductType() != null ? dto.getProductType() : "STANDARD");

        // Relaciones (Category, Supplier) - IGUAL QUE ANTES
        if (dto.getCategoryId() != null) {
            Category cat = categoryRepository.findById(dto.getCategoryId()).orElseThrow();
            if (!cat.getTenantId().equals(tenantId)) return ResponseEntity.status(403).body("Categoría ajena");
            product.setCategory(cat);
        }
        if (dto.getSupplierId() != null) {
            Supplier sup = supplierRepository.findById(dto.getSupplierId()).orElseThrow();
            if (!sup.getTenantId().equals(tenantId)) return ResponseEntity.status(403).body("Proveedor ajeno");
            product.setSupplier(sup);
        }

        // --- LÓGICA DE PACKS MEJORADA ---
        // Si viene null, lo tratamos como NULL (Ilimitado/Virtual)
        BigDecimal inputStock = dto.getStockCurrent();
        BigDecimal stockToSave;

        if ("BUNDLE".equals(product.getProductType())) {
            // Si es PACK y ponen 0, guardamos NULL (Infinito/Virtual)
            stockToSave = (inputStock != null && inputStock.compareTo(BigDecimal.ZERO) == 0)
                    ? null
                    : inputStock;
        } else {
            // Si es PRODUCTO NORMAL, 0 es 0
            stockToSave = inputStock != null ? inputStock : BigDecimal.ZERO;
        }

        if ("BUNDLE".equals(product.getProductType())) {
            if (dto.getBundleItems() == null || dto.getBundleItems().isEmpty()) {
                return ResponseEntity.badRequest().body("Un Pack debe tener al menos un producto.");
            }

            for (ProductDto.BundleItemDto itemDto : dto.getBundleItems()) {
                Product component = productRepository.findById(itemDto.getComponentId())
                        .orElseThrow(() -> new RuntimeException("Componente no encontrado"));

                // VALIDAR DISPONIBILIDAD INICIAL (Solo si es una promoción limitada)
                if (stockToSave != null) {
                    BigDecimal requiredQty = stockToSave.multiply(itemDto.getQuantity());
                    if (component.getStockCurrent().compareTo(requiredQty) < 0) {
                        return ResponseEntity.badRequest().body(
                                "Stock insuficiente en '" + component.getName() + "'. Necesitas " + requiredQty
                        );
                    }
                }
                product.addBundleItem(component, itemDto.getQuantity());
            }
            product.setStockCurrent(stockToSave); // Guardamos NULL o el Número
        } else {
            product.setStockCurrent(stockToSave);
        }

        Product savedProduct = productRepository.save(product);

        // LOG
        if (stockToSave != null && stockToSave.compareTo(BigDecimal.ZERO) > 0) {
            createInventoryLog(savedProduct, currentUser, "CREATE", stockToSave, BigDecimal.ZERO, stockToSave, "Inventario inicial");
        }

        return ResponseEntity.ok(mapToDto(savedProduct));
    }

    // ... (GET getProductById, getProductBySku IGUALES) ...
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable UUID id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new RuntimeException("No encontrado"));
        return ResponseEntity.ok(mapToDto(product));
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<ProductDto> getProductBySku(@PathVariable String sku) {
        UUID tenantId = getCurrentTenantId();
        Product product = productRepository.findByTenantId(tenantId).stream()
                .filter(p -> p.getSku().equals(sku)).findFirst().orElse(null);
        if (product == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(mapToDto(product));
    }

    // 3. Actualizar Producto (PUT)
    @PutMapping("/{id}")
    @Transactional // Importante para consistencia
    public ResponseEntity<?> updateProduct(@PathVariable UUID id, @RequestBody ProductDto dto) {
        UUID tenantId = getCurrentTenantId();
        User currentUser = getCurrentUserEntity();

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (!product.getTenantId().equals(tenantId)) {
            return ResponseEntity.status(403).body("No tienes permiso");
        }

        // --- 1. LÓGICA DE STOCK (0 -> NULL para Packs) ---
        BigDecimal oldStock = product.getStockCurrent();
        BigDecimal inputStock = dto.getStockCurrent();
        BigDecimal newStock;

        // Si es PACK y envían 0, guardamos NULL (Infinito)
        if ("BUNDLE".equals(product.getProductType())) {
            newStock = (inputStock != null && inputStock.compareTo(BigDecimal.ZERO) == 0)
                    ? null
                    : inputStock;
        } else {
            // Si es normal, 0 es 0
            newStock = inputStock;
        }

        // --- AUDITORÍA ---
        // Para comparar, tratamos NULL como 0
        BigDecimal oldSafe = oldStock != null ? oldStock : BigDecimal.ZERO;
        BigDecimal newSafe = newStock != null ? newStock : BigDecimal.ZERO;

        if (newSafe.compareTo(oldSafe) != 0) {
            BigDecimal diff = newSafe.subtract(oldSafe);
            createInventoryLog(product, currentUser, "MANUAL_ADJUST", diff, oldSafe, newSafe, "Ajuste manual desde Catálogo");
        }

        // --- 2. ACTUALIZACIÓN DE CAMPOS BÁSICOS ---
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setStockCurrent(newStock); // Guardamos el valor procesado (NULL o Número)
        product.setStockMin(dto.getStockMin());
        product.setMeasurementUnit(dto.getMeasurementUnit());
        calculateProductPrices(product, dto);
        product.setImageUrl(dto.getImageUrl());
        product.setIsPublic(dto.getIsPublic() != null ? dto.getIsPublic() : false);
        product.setAttributes(dto.getAttributes());

        if (dto.getCategoryId() != null) {
            Category cat = categoryRepository.findById(dto.getCategoryId()).orElseThrow();
            if (cat.getTenantId().equals(tenantId)) product.setCategory(cat);
        }
        if (dto.getSupplierId() != null) {
            Supplier sup = supplierRepository.findById(dto.getSupplierId()).orElseThrow();
            if (sup.getTenantId().equals(tenantId)) product.setSupplier(sup);
        }

        // --- 3. ACTUALIZACIÓN DE ITEMS DEL PACK ---
        if ("BUNDLE".equals(product.getProductType())) {
            // A. Limpiar lista actual (Borrar antiguos)
            product.getBundleItems().clear();

            // B. Agregar los nuevos
            if (dto.getBundleItems() != null) {
                for (ProductDto.BundleItemDto itemDto : dto.getBundleItems()) {
                    Product component = productRepository.findById(itemDto.getComponentId())
                            .orElseThrow(() -> new RuntimeException("Componente no encontrado: " + itemDto.getComponentId()));

                    product.addBundleItem(component, itemDto.getQuantity());
                }
            }
        }
        // ----------------------------------------------------

        Product savedProduct = productRepository.save(product);

        return ResponseEntity.ok(mapToDto(savedProduct));
    }

    // 4. Eliminar Producto (CORREGIDO HARD DELETE)
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteProduct(@PathVariable UUID id,
                                           @RequestParam(defaultValue = "false") boolean force) {
        UUID tenantId = getCurrentTenantId();
        Product product = productRepository.findById(id).orElseThrow(() -> new RuntimeException("No encontrado"));
        if (!product.getTenantId().equals(tenantId)) return ResponseEntity.status(403).body("Sin permiso");

        if (force) {
            try {
                // 1. Borrar items de órdenes
                webOrderRepository.deleteByProductId(id);
                webOrderItemRepository.deleteByProductId(id);
                saleItemRepository.deleteByProductId(id);

                // 2. BORRAR RELACIONES DE PACKS (ESTO SOLUCIONA TU ERROR)
                // A. Si soy un Pack, borra mis hijos de la tabla intermedia
                bundleItemRepository.deleteByBundleProductId(id);
                // B. Si soy un Componente, borra mis apariciones en otros packs
                bundleItemRepository.deleteByComponentProductId(id);

                // 3. Borrar físicamente
                productRepository.deleteHard(id);
                return ResponseEntity.ok().body("{\"message\": \"Eliminado definitivamente.\"}");
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(409).body("Error al eliminar: " + e.getMessage());
            }
        } else {
            productRepository.deleteById(id);
            return ResponseEntity.ok().body("{\"message\": \"Archivado correctamente.\"}");
        }
    }

    // ... (activateProduct, getLowStockProducts, calculateProductPrices, createInventoryLog, getCurrentTenantId, getCurrentUserEntity IGUALES) ...
    // Solo asegurate de que estén en el archivo final.

    // PATCH /activate
    @PatchMapping("/{id}/activate")
    public ResponseEntity<?> activateProduct(@PathVariable UUID id) {
        UUID tenantId = getCurrentTenantId();
        Product product = productRepository.findAnyStatusById(id).orElseThrow();
        if (!product.getTenantId().equals(tenantId)) return ResponseEntity.status(403).build();
        productRepository.activateProduct(id);
        return ResponseEntity.ok().body("{\"message\": \"Activado.\"}");
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<ProductDto>> getLowStockProducts() {
        UUID tenantId = getCurrentTenantId();
        List<Product> all = productRepository.findByTenantId(tenantId);
        return ResponseEntity.ok(all.stream().filter(p -> p.getStockCurrent().compareTo(p.getStockMin()) <= 0).map(this::mapToDto).collect(Collectors.toList()));
    }

    private void calculateProductPrices(Product product, ProductDto dto) {
        BigDecimal cost = dto.getCostPrice() != null ? dto.getCostPrice() : BigDecimal.ZERO;
        BigDecimal inputPrice = dto.getPriceFinal() != null ? dto.getPriceFinal() : BigDecimal.ZERO;
        BigDecimal taxPct = dto.getTaxPercent() != null ? dto.getTaxPercent() : new BigDecimal("19.0");
        boolean hasTax = dto.getIsTaxIncluded() != null ? dto.getIsTaxIncluded() : true;

        product.setCostPrice(cost);
        product.setPriceFinal(inputPrice);
        product.setIsTaxIncluded(hasTax);
        product.setTaxPercent(taxPct);

        BigDecimal priceNeto;
        if (hasTax) {
            BigDecimal taxFactor = BigDecimal.ONE.add(taxPct.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
            priceNeto = inputPrice.divide(taxFactor, 4, RoundingMode.HALF_UP);
        } else {
            priceNeto = inputPrice;
        }
        product.setPriceNeto(priceNeto);
    }

    private void createInventoryLog(Product p, User u, String action, BigDecimal change, BigDecimal oldStk, BigDecimal newStk, String reason) {
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
        inventoryLogRepository.save(log);
    }

    private UUID getCurrentTenantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        return user.getTenant().getId();
    }

    private User getCurrentUserEntity() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName()).orElseThrow();
    }

    // --- CORRECCIÓN EN EL MAPPER PARA VER LOS ITEMS ---
    private ProductDto mapToDto(Product p) {
        ProductDto dto = new ProductDto();
        // ... Campos básicos (id, sku, name, description, prices...)
        dto.setId(p.getId());
        dto.setSku(p.getSku());
        dto.setName(p.getName());
        dto.setDescription(p.getDescription());
        dto.setPriceNeto(p.getPriceNeto());
        dto.setTaxPercent(p.getTaxPercent());
        dto.setCostPrice(p.getCostPrice());
        dto.setPriceFinal(p.getPriceFinal());
        dto.setIsTaxIncluded(p.getIsTaxIncluded());

        // Margen
        if (p.getCostPrice() != null && p.getCostPrice().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal ganancia = p.getPriceNeto().subtract(p.getCostPrice());
            BigDecimal margen = ganancia.divide(p.getCostPrice(), 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
            dto.setMarginPercent(margen.setScale(2, RoundingMode.HALF_UP));
        } else {
            dto.setMarginPercent(new BigDecimal("100"));
        }

        dto.setStockCurrent(p.getEffectiveStock());
        dto.setStockMin(p.getStockMin());
        dto.setAttributes(p.getAttributes());
        dto.setMeasurementUnit(p.getMeasurementUnit());
        dto.setImageUrl(p.getImageUrl());
        dto.setIsActive(p.isActive());
        dto.setIsPublic(p.getIsPublic());

        if (p.getCategory() != null) {
            dto.setCategoryId(p.getCategory().getId());
            dto.setCategoryName(p.getCategory().getName());
        }
        if (p.getSupplier() != null) {
            dto.setSupplierId(p.getSupplier().getId());
            dto.setSupplierName(p.getSupplier().getName());
        }

        // --- MAPEO DEL PACK ---
        dto.setProductType(p.getProductType());
        if ("BUNDLE".equals(p.getProductType()) && p.getBundleItems() != null) {
            List<ProductDto.BundleItemDto> itemsDto = p.getBundleItems().stream().map(bi -> {
                ProductDto.BundleItemDto item = new ProductDto.BundleItemDto();
                item.setComponentId(bi.getComponentProduct().getId());
                item.setQuantity(bi.getQuantity());
                // Datos visuales para el front
                item.setComponentName(bi.getComponentProduct().getName());
                item.setComponentSku(bi.getComponentProduct().getSku());
                return item;
            }).collect(Collectors.toList());
            dto.setBundleItems(itemsDto);
        }
        // ----------------------

        return dto;
    }
}