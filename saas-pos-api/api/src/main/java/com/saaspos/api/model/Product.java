package com.saaspos.api.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Data
@Entity
@Table(name = "products")
@SQLDelete(sql = "UPDATE products SET is_active = false WHERE id = ?")
public class Product {

    @ManyToOne(fetch = FetchType.EAGER) // Eager para mostrar el nombre de la categoría en la tabla
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    //Importante: Multi-tenancy
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "cost_price")
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Column(name = "price_neto", nullable = false)
    private BigDecimal priceNeto;

    @Column(name = "price_final")
    private BigDecimal priceFinal; // El número que escribió el usuario (Ej: 1190)

    @Column(name = "is_tax_included")
    private Boolean isTaxIncluded = true; // ¿Ese 1190 tiene IVA?

    @Column(name = "tax_percent")
    private BigDecimal taxPercent = new BigDecimal("19.0");

    @Column(name = "stock_current")
    private BigDecimal stockCurrent = BigDecimal.ZERO;

    @Column(name = "stock_min")
    private BigDecimal stockMin = new BigDecimal("5.0");

    @Column(name = "measurement_unit")
    private String measurementUnit = "UNIT";

    //JSONB: mapeamos la columna 'attributes' a un Map de Java
    //Esto permite guardar {"color":"rojo", "peso":"1kg"} dinamicamente
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", columnDefinition = "jsonb")
    private Map<String, Object> attributes = new HashMap<>();

    @Column(name = "is_active")
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Transient
    private BigDecimal calculatedMargin;

    // --- CAMPOS E-COMMERCE ---

    @Column(name = "is_public")
    private Boolean isPublic = false; // ¿Se muestra en la web?

    @Column(name = "public_price")
    private BigDecimal publicPrice; // Opcional: Precio diferenciado para web

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "description_web")
    private String descriptionWeb;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductPriceHistory> priceHistory;

    // NUEVO: Tipo de producto
    @Column(name = "product_type")
    private String productType = "STANDARD"; // STANDARD, BUNDLE

    // NUEVO: Relación con los items del pack (Si este producto es un pack)
    @OneToMany(mappedBy = "bundleProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BundleItem> bundleItems = new ArrayList<>();

    // Helper para agregar componentes
    public void addBundleItem(Product component, BigDecimal qty) {
        BundleItem item = new BundleItem();
        item.setBundleProduct(this);
        item.setComponentProduct(component);
        item.setQuantity(qty);
        this.bundleItems.add(item);
    }

    /**
     * Calcula el stock REAL disponible para la venta.
     * Si es ESTÁNDAR: Devuelve el stock físico.
     * Si es PACK: Calcula cuántos se pueden armar según los componentes y el límite manual.
     */
    public BigDecimal getEffectiveStock() {
        if (!"BUNDLE".equals(this.productType)) {
            return this.stockCurrent != null ? this.stockCurrent : BigDecimal.ZERO;
        }

        // CAMBIO CLAVE:
        // Si es NULL -> Es "Infinito" (Pack Virtual).
        // Si es 0 -> Es "Agotado" (Pack Limitado que se acabó).
        BigDecimal limit = (this.stockCurrent == null)
                ? new BigDecimal("999999")
                : this.stockCurrent;

        // 2. Revisamos cada componente
        if (this.bundleItems != null) {
            for (BundleItem item : this.bundleItems) {
                Product component = item.getComponentProduct();
                BigDecimal componentStock = component.getStockCurrent() != null ? component.getStockCurrent() : BigDecimal.ZERO;
                BigDecimal qtyNeededPerPack = item.getQuantity();

                if (qtyNeededPerPack.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal maxPacksFromComponent = componentStock.divide(qtyNeededPerPack, 0, RoundingMode.FLOOR);

                    if (maxPacksFromComponent.compareTo(limit) < 0) {
                        limit = maxPacksFromComponent;
                    }
                }
            }
        }

        return limit;
    }

}
