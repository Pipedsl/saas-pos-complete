package com.saaspos.api.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "product_price_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductPriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación con el Producto (Muchos historiales pertenecen a un producto)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Precio anterior (Opcional, pero útil para calcular variaciones rápidas)
    @Column(name = "old_price", precision = 19, scale = 2)
    private BigDecimal oldPrice;

    // Nuevo precio establecido
    @Column(name = "new_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal newPrice;

    // Fecha exacta del cambio
    @CreationTimestamp
    @Column(name = "change_date", nullable = false, updatable = false)
    private LocalDateTime changeDate;

    // Motivo del cambio (Ej: "Actualización manual", "Importación masiva", "Oferta")
    @Column(name = "change_reason")
    private String changeReason;

    // Usuario que realizó el cambio (Auditoría básica)
    @Column(name = "changed_by_user_id")
    private Long changedByUserId;

    // -------------------------------------------------
    // CRÍTICO PARA SAAS MULTI-TENANT
    // -------------------------------------------------
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
}