package com.saaspos.api.model.ecommerce;

import com.saaspos.api.model.Sale;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "web_orders")
public class WebOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "order_number", nullable = false)
    private String orderNumber; // Ej: "WEB-1024"

    // --- Datos Cliente ---
    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_phone", nullable = false)
    private String customerPhone;

    @Column(name = "customer_rut")
    private String customerRut;

    @Column(name = "shipping_region")
    private String shippingRegion;

    @Column(name = "shipping_commune")
    private String shippingCommune;

    @Column(name = "shipping_street")
    private String shippingStreet;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "shipping_address")
    private String shippingAddress;

    @Column(name = "shipping_notes")
    private String shippingNotes;

    // --- Estado ---
    // Usaremos String por simplicidad, o podrías crear un Enum
    @Column(nullable = false)
    private String status; // PENDING, EDITED, PAID, DELIVERED, CANCELLED, EXPIRED

    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // Límite para pagar (Reserva)

    // --- Auditoría de Edición ---
    @Column(name = "was_edited")
    private boolean wasEdited = false;

    @Column(name = "edited_by_user_id")
    private UUID editedByUserId;

    @Column(name = "edit_reason")
    private String editReason;

    // --- Finanzas ---
    @Column(name = "total_items")
    private BigDecimal totalItems = BigDecimal.ZERO;

    @Column(name = "shipping_cost")
    private BigDecimal shippingCost = BigDecimal.ZERO;

    @Column(name = "final_total")
    private BigDecimal finalTotal = BigDecimal.ZERO;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "shipping_method")
    private String shippingMethod;

    @Column(name = "courier")
    private String courier; // Ej: "starken", "chilexpress"

    // --- Relaciones ---
    @OneToOne
    @JoinColumn(name = "sale_id")
    private Sale sale; // Se llena solo cuando se confirma el pago

    @OneToMany(mappedBy = "webOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WebOrderItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}