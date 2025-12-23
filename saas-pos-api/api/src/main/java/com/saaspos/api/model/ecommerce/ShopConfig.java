package com.saaspos.api.model.ecommerce;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@Entity
@Table(name = "shop_configs")
public class ShopConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "url_slug", nullable = false, unique = true)
    private String urlSlug; // Identificador único URL

    @Column(name = "shop_name")
    private String shopName;

    @Column(name = "primary_color")
    private String primaryColor = "#000000";

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "banner_url")
    private String bannerUrl;

    @Column(name = "reservation_minutes")
    private Integer reservationMinutes = 60;

    // Configuración flexible usando JSONB
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payment_methods", columnDefinition = "jsonb")
    private Map<String, Object> paymentMethods = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "shipping_methods", columnDefinition = "jsonb")
    private Map<String, Object> shippingMethods = new HashMap<>();

    @Column(name = "is_active")
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}