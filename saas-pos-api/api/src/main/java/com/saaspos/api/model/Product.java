package com.saaspos.api.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@Entity
@Table(name = "products")
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

    @Column(name = "image_url")
    private String imageUrl;

}
