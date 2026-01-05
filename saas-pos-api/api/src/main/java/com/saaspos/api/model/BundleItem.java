package com.saaspos.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Entity
@Table(name = "bundle_items")
public class BundleItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bundle_product_id", nullable = false)
    @JsonIgnore // Evitar ciclo infinito al serializar
    private Product bundleProduct;

    @ManyToOne(fetch = FetchType.EAGER) // Queremos ver los detalles del hijo al cargar el pack
    @JoinColumn(name = "component_product_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "bundleItems"})
    private Product componentProduct;

    @Column(nullable = false)
    private BigDecimal quantity;
}