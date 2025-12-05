package com.saaspos.api.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Entity
@Table(name = "plans")
public class Plan {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name; // BASIC, PRO, FULL

    @Column(name = "price_clp")
    private BigDecimal priceClp;

    @Column(name = "max_products")
    private Integer maxProducts;

    @Column(name = "max_users")
    private Integer maxUsers;

    // Otros flags de features
    @Column(name = "enable_sii")
    private boolean enableSii;
}