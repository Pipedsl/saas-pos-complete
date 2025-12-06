package com.saaspos.api.controller;

import com.saaspos.api.dto.ProductDto;
import com.saaspos.api.model.*;
import com.saaspos.api.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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

    public ProductController(ProductRepository productRepository, UserRepository userRepository, TenantRepository tenantRepository, CategoryRepository categoryRepository, SupplierRepository supplierRepository) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
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
        product.setPriceNeto(dto.getPriceNeto());
        product.setTaxPercent(dto.getTaxPercent() != null ? dto.getTaxPercent() : new BigDecimal("19.0"));
        product.setStockCurrent(dto.getStockCurrent() != null ? dto.getStockCurrent() : BigDecimal.ZERO);
        product.setStockMin(dto.getStockMin() != null ? dto.getStockMin() : new BigDecimal("5"));
        product.setAttributes(dto.getAttributes()); //Guardar JSONB
        product.setCostPrice(dto.getCostPrice() != null ? dto.getCostPrice() : BigDecimal.ZERO);
        product.setMeasurementUnit(dto.getMeasurementUnit() != null ? dto.getMeasurementUnit() : "UNIT");

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
        product.setPriceNeto(dto.getPriceNeto());
        product.setTaxPercent(dto.getTaxPercent());
        product.setStockCurrent(dto.getStockCurrent());
        product.setMeasurementUnit(dto.getMeasurementUnit());
        product.setCostPrice(dto.getCostPrice() != null ? dto.getCostPrice() : BigDecimal.ZERO);

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
    public ResponseEntity<?> deleteProduct(@PathVariable UUID id) {
        UUID tenantId = getCurrentTenantId();
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (!product.getTenantId().equals(tenantId)) {
            return ResponseEntity.status(403).body("No tienes permiso");
        }

        productRepository.delete(product);
        return ResponseEntity.ok().build();
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
        dto.setStockCurrent(p.getStockCurrent());
        dto.setStockMin(p.getStockMin());
        dto.setAttributes(p.getAttributes());
        dto.setMeasurementUnit(p.getMeasurementUnit());
        dto.setCostPrice(p.getCostPrice());
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
