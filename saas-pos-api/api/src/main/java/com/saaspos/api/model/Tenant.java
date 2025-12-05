package com.saaspos.api.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER) // Eager para tener los límites a mano siempre
    @JoinColumn(name = "plan_id")
    private Plan plan;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(nullable = false)
    private String rut;

    @Column(name = "is_active")
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", columnDefinition = "jsonb")
    private Map<String, String> settings = new HashMap<>();

    @Column(name = "is_demo_account")
    private Boolean isDemoAccount = false;

    @Column(name = "demo_expires_at")
    private LocalDateTime demoExpiresAt;
}
