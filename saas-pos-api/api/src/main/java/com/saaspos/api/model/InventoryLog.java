package com.saaspos.api.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "inventory_logs")
public class InventoryLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "product_name_snapshot")
    private String productNameSnapshot;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "user_name_snapshot")
    private String userNameSnapshot;

    // Tipos: 'MANUAL', 'SALE', 'WEB_ORDER', 'SALE_EDIT', 'WEB_ORDER_EDIT', 'RETURN'
    @Column(name = "action_type")
    private String actionType;

    @Column(name = "quantity_change")
    private BigDecimal quantityChange;

    @Column(name = "old_stock")
    private BigDecimal oldStock;

    @Column(name = "new_stock")
    private BigDecimal newStock;

    @Column(name = "reason")
    private String reason;

    // --- NUEVAS VINCULACIONES (V21) ---
    @Column(name = "sale_id")
    private UUID saleId;        // ID del Ticket POS

    @Column(name = "web_order_id")
    private UUID webOrderId;    // ID del Pedido Web
    // ----------------------------------

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}