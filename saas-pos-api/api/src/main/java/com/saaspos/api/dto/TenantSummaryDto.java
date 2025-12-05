package com.saaspos.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class TenantSummaryDto {
    private UUID id;
    private String companyName;
    private String ownerName;    // Nombre del dueño (usuario admin)
    private String ownerEmail;   // Email del dueño
    private String planName;     // PLAN_DEMO, PRO, etc.
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime demoExpiresAt;

    // Opcional: Para saber quién lo vendió (lo sacaremos del DemoLink)
    private String referredByAgent;
}