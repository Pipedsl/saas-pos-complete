package com.saaspos.api.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "sales")
public class Sale {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "sale_number") // Folio (opcional por ahora)
    private String saleNumber;

    @Column(name = "subtotal_amount", nullable = false)
    private BigDecimal subtotalAmount;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "total_tax")
    private BigDecimal totalTax;

    @Column(name = "status")
    private String status = "COMPLETED"; // COMPLETED, CANCELLED

    @Column(name = "was_edited")
    private boolean wasEdited = false;

    @Column(name = "edited_by_user_id")
    private UUID editedByUserId;

    @Column(name = "edit_reason")
    private String editReason;

    // Relación con los ítems (Cascada: si guardo venta, guarda ítems)
    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Helper para agregar ítems y mantener la relación bidireccional
    public void addItem(SaleItem item) {
        items.add(item);
        item.setSale(this);
    }
}