package com.saaspos.api.model.ecommerce;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.saaspos.api.model.Product;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Entity
@Table(name = "web_order_items")
public class WebOrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "web_order_id", nullable = false)
    @JsonIgnore
    private WebOrder webOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Product product;

    // --- Snapshots (Historial Fidedigno) ---
    @Column(name = "product_name_snapshot")
    private String productNameSnapshot;

    @Column(name = "sku_snapshot")
    private String skuSnapshot;

    // --- Cantidades y Precios ---
    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(name = "unit_price_at_moment", nullable = false)
    private BigDecimal unitPriceAtMoment; // Precio Venta

    @Column(name = "cost_price_at_moment", nullable = false)
    private BigDecimal costPriceAtMoment; // Costo (Para calcular margen)

    @Column(nullable = false)
    private BigDecimal subtotal;
}