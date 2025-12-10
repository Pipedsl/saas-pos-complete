package com.saaspos.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Entity
@Table(name = "sale_items")
public class SaleItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    @JsonIgnore
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private BigDecimal quantity; // Puede ser decimal (KG)

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "unit_tax")
    private BigDecimal unitTax;

    @Column(name = "cost_price_at_sale")
    private BigDecimal costPriceAtSale;

    @Column(name = "net_price_at_sale")
    private BigDecimal netPriceAtSale; // Precio Neto real (Base Imponible)

    @Column(name = "tax_amount_at_sale")
    private BigDecimal taxAmountAtSale;
}