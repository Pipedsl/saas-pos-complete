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
    private final ProductPriceHistoryRepository productPriceHistoryRepository;

    public ProductController(ProductRepository productRepository, UserRepository userRepository, TenantRepository tenantRepository, CategoryRepository categoryRepository, SupplierRepository supplierRepository, SaleItemRepository saleItemRepository, WebOrderRepository webOrderRepository, WebOrderItemRepository webOrderItemRepository, ProductPriceHistoryRepository productPriceHistoryRepository) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
        this.saleItemRepository = saleItemRepository;
        this.webOrderRepository = webOrderRepository;
        this.webOrderItemRepository = webOrderItemRepository;
        this.productPriceHistoryRepository = productPriceHistoryRepository;
    }

    //GET: Listar mis productos
    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        UUID tenantId = getCurrentTenantId();

        List<Product> products = productRepository.findByTenantId(tenantId);

        //Convertir Entidad -> DTO
        List<ProductDto> dtos = products.stream().map(this::mapToDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    //POST: Crear nuevo producto
    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody ProductDto dto) {
        UUID tenantId = getCurrentTenantId();

        // 1. OBTENER EL TENANT Y SU PLAN
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant no existe"));

        Plan plan = tenant.getPlan();

        // 2. CONTAR PRODUCTOS ACTUALES
        long currentProducts = productRepository.countByTenantId(tenantId); // Necesitas agregar este método al repo si no existe

        // 3. VALIDAR REGLA DE NEGOCIO (Plan Limits)
        if (plan != null && currentProducts >= plan.getMaxProducts()) {
            return ResponseEntity.status(403)
                    .body("Has alcanzado el límite de productos de tu plan (" + plan.getName() + ": " + plan.getMaxProducts() + "). Actualiza a PRO para continuar.");
        }

        //Validacion de unidad de SKU por empresa
        if (productRepository.existsByTenantIdAndSku(tenantId, dto.getSku())) {
            return ResponseEntity.badRequest().body("El SKU '" + dto.getSku() + "' ya existe en tu inventario.");
        }

        Product product = new Product();
        product.setTenantId(tenantId); //Asignacion automatica de tenant
        product.setSku(dto.getSku());
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        calculateProductPrices(product, dto);
        product.setStockCurrent(dto.getStockCurrent() != null ? dto.getStockCurrent() : BigDecimal.ZERO);
        product.setStockMin(dto.getStockMin() != null ? dto.getStockMin() : new BigDecimal("5"));
        product.setAttributes(dto.getAttributes()); //Guardar JSONB
        product.setMeasurementUnit(dto.getMeasurementUnit() != null ? dto.getMeasurementUnit() : "UNIT");
        product.setImageUrl(dto.getImageUrl());

        product.setActive(true);

        if (dto.getCategoryId() != null) {
            Category cat = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));

            //seguridad: Verificar que la categoria sea del mismo tenant
            if (!cat.getTenantId().equals(tenantId)) {
                return ResponseEntity.status(403).body("No puedes usar una categoría de otra empresa");
            }
            product.setCategory(cat);
        }

        if (dto.getSupplierId() != null) {
            Supplier sup = supplierRepository.findById(dto.getSupplierId())
                    .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));

            // Seguridad: Verificar que sea MI proveedor
            if (!sup.getTenantId().equals(tenantId)) {
                return ResponseEntity.status(403).body("No puedes usar un proveedor ajeno");
            }
            product.setSupplier(sup);
        }

        productRepository.save(product);

        return ResponseEntity.ok(mapToDto(product));

    }

    // 1. Obtener por ID (Para cargar el formulario de edición)
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        return ResponseEntity.ok(mapToDto(product));
    }

    // 2. Buscar por SKU (Para la redirección inteligente)
    @GetMapping("/sku/{sku}")
    public ResponseEntity<ProductDto> getProductBySku(@PathVariable String sku) {
        UUID tenantId = getCurrentTenantId();
        // Nota: Necesitas agregar findByTenantIdAndSku en el Repository si no existe, o usar Example
        // Asumiremos que creas este método en el Repo o filtras la lista.
        // Forma rápida con Streams (no óptima para millones de datos pero sirve ahora):
        Product product = productRepository.findByTenantId(tenantId).stream()
                .filter(p -> p.getSku().equals(sku))
                .findFirst()
                .orElse(null);

        if (product == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(mapToDto(product));
    }

    // 3. Actualizar Producto (PUT)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable UUID id, @RequestBody ProductDto dto) {
        UUID tenantId = getCurrentTenantId();
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (!product.getTenantId().equals(tenantId)) {
            return ResponseEntity.status(403).body("No tienes permiso");
        }

        // Actualizar campos
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setStockCurrent(dto.getStockCurrent());
        product.setStockMin(dto.getStockMin());
        product.setMeasurementUnit(dto.getMeasurementUnit());
        calculateProductPrices(product, dto);
        product.setImageUrl(dto.getImageUrl());

        // Atributos y Categoría
        product.setAttributes(dto.getAttributes());
        if (dto.getCategoryId() != null) {
            Category cat = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));

            //seguridad: Verificar que la categoria sea del mismo tenant
            if (!cat.getTenantId().equals(tenantId)) {
                return ResponseEntity.status(403).body("No puedes usar una categoría de otra empresa");
            }
            product.setCategory(cat);
        }
        if (dto.getSupplierId() != null) {
            Supplier sup = supplierRepository.findById(dto.getSupplierId())
                    .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));

            // Seguridad: Verificar que sea MI proveedor
            if (!sup.getTenantId().equals(tenantId)) {
                return ResponseEntity.status(403).body("No puedes usar un proveedor ajeno");
            }
            product.setSupplier(sup);
        }

        productRepository.save(product);
        return ResponseEntity.ok(mapToDto(product));
    }

    // 4. Eliminar Producto (DELETE)
    @DeleteMapping("/{id}")
    @Transactional // Importante para que el borrado sea atómico
    public ResponseEntity<?> deleteProduct(@PathVariable UUID id,
                                           @RequestParam(defaultValue = "false") boolean force) {

        UUID tenantId = getCurrentTenantId();
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (!product.getTenantId().equals(tenantId)) {
            return ResponseEntity.status(403).body("No tienes permiso");
        }

        if (force) {
            try {
            // --- HARD DELETE (Destructivo) ---
                // 1. Borrar historial de items de venta de este producto

                webOrderRepository.deleteByProductId(id);
                // 1. Borrar items del e-commerce vinculados
                webOrderItemRepository.deleteByProductId(id);

                // 2. Borrar items de ventas POS vinculados
                saleItemRepository.deleteByProductId(id);


            // 2. Borrar el producto físicamente
                productRepository.deleteHard(id);

            return ResponseEntity.ok().body("{\"message\": \"Producto y su historial eliminados definitivamente.\"}");

            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(409).body("No se pudo eliminar: " + e.getMessage());
            }


        } else {
            // --- SOFT DELETE (Archivar) ---
            productRepository.deleteById(id);
            return ResponseEntity.ok().body("{\"message\": \"Producto archivado correctamente.\"}");
        }
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<?> activateProduct(@PathVariable UUID id) {
        UUID tenantId = getCurrentTenantId();

        // CAMBIO IMPORTANTE: Usamos findAnyStatusById en lugar de findById
        Product product = productRepository.findAnyStatusById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        // Verificamos que pertenezca a tu empresa
        if (!product.getTenantId().equals(tenantId)) {
            return ResponseEntity.status(403).body("No tienes permiso");
        }

        // Si ya lo encontramos y validamos, lo reactivamos
        productRepository.activateProduct(id);

        return ResponseEntity.ok().body("{\"message\": \"Producto reactivado exitosamente.\"}");
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<ProductDto>> getLowStockProducts() {
        UUID tenantId = getCurrentTenantId();
        // Usamos streams para filtrar rápido (o podrías hacer una query nativa en el repo)
        List<Product> all = productRepository.findByTenantId(tenantId);
        List<ProductDto> lowStock = all.stream()
                .filter(p -> p.getStockCurrent().compareTo(p.getStockMin()) <= 0)
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(lowStock);
    }

    private void calculateProductPrices(Product product, ProductDto dto) {
        // 1. Obtener valores seguros (evitar NullPointerException)
        BigDecimal cost = dto.getCostPrice() != null ? dto.getCostPrice() : BigDecimal.ZERO;
        BigDecimal inputPrice = dto.getPriceFinal() != null ? dto.getPriceFinal() : BigDecimal.ZERO;
        // Por defecto el impuesto es 19% si no viene
        BigDecimal taxPct = dto.getTaxPercent() != null ? dto.getTaxPercent() : new BigDecimal("19.0");
        // Por defecto asumimos que incluye IVA si no se especifica
        boolean hasTax = dto.getIsTaxIncluded() != null ? dto.getIsTaxIncluded() : true;

        // 2. Asignar los valores directos a la entidad
        product.setCostPrice(cost);
        product.setPriceFinal(inputPrice);
        product.setIsTaxIncluded(hasTax);
        product.setTaxPercent(taxPct);

        // 3. CALCULAR EL PRECIO NETO (La verdad contable)
        BigDecimal priceNeto;

        if (hasTax) {
            // Factor 1.19
            BigDecimal taxFactor = BigDecimal.ONE.add(taxPct.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));

            // CAMBIO AQUÍ: Usar escala 4 en la división (84.0336 en vez de 84)
            priceNeto = inputPrice.divide(taxFactor, 4, RoundingMode.HALF_UP);
        } else {
            priceNeto = inputPrice;
        }

        product.setPriceNeto(priceNeto);
    }

    private  UUID getCurrentTenantId(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (user.getTenant() == null){
            throw new RuntimeException("El usuario no pertenece a ninguna empresa (Es Super Admin Global?)");
        }
        return user.getTenant().getId();
    }



    private  ProductDto mapToDto(Product p) {
        ProductDto dto = new ProductDto();
        dto.setId(p.getId());
        dto.setSku(p.getSku());
        dto.setName(p.getName());
        dto.setDescription(p.getDescription());

        dto.setPriceNeto(p.getPriceNeto());
        dto.setTaxPercent(p.getTaxPercent());
        dto.setCostPrice(p.getCostPrice());

        dto.setPriceFinal(p.getPriceFinal());
        dto.setIsTaxIncluded(p.getIsTaxIncluded());

        if (p.getCostPrice() != null && p.getCostPrice().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal ganancia = p.getPriceNeto().subtract(p.getCostPrice());
            BigDecimal margen = ganancia.divide(p.getCostPrice(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            // Redondear a 2 decimales para que se vea bonito (ej: 33.50%)
            dto.setMarginPercent(margen.setScale(2, RoundingMode.HALF_UP));
        } else {
            // Si el costo es 0, el margen es 100% (o infinito)
            dto.setMarginPercent(new BigDecimal("100"));
        }

        dto.setStockCurrent(p.getStockCurrent());
        dto.setStockMin(p.getStockMin());
        dto.setAttributes(p.getAttributes());
        dto.setMeasurementUnit(p.getMeasurementUnit());
        dto.setImageUrl(p.getImageUrl());
        dto.setIsActive(p.isActive());
        if (p.getCategory() != null) {
            dto.setCategoryId(p.getCategory().getId());
            dto.setCategoryName(p.getCategory().getName());
        }
        if (p.getSupplier() != null) {
            dto.setSupplierId(p.getSupplier().getId());
            dto.setSupplierName(p.getSupplier().getName());
        }

        return dto;
    }
}
