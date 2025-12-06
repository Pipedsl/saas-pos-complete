package com.saaspos.api.controller;

import com.saaspos.api.dto.SubscriptionUpdateDto;
import com.saaspos.api.dto.TenantSummaryDto;
import com.saaspos.api.model.Plan;
import com.saaspos.api.model.Tenant;
import com.saaspos.api.model.User;
import com.saaspos.api.repository.PlanRepository;
import com.saaspos.api.repository.TenantRepository;
import com.saaspos.api.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class SuperAdminController {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;

    public SuperAdminController(TenantRepository tenantRepository, UserRepository userRepository, PlanRepository planRepository) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.planRepository = planRepository;
    }

    @GetMapping("/tenants")
    public ResponseEntity<?> getAllTenants() {
        // 1. Validar Seguridad (Doble chequeo)
        User currentUser = getCurrentUser();
        if (!currentUser.getRole().equals("SUPER_ADMIN")) {
            return ResponseEntity.status(403).body("Acceso denegado. Zona restringida.");
        }

        // 2. Obtener lista
        List<TenantSummaryDto> tenants = tenantRepository.findAllTenantSummaries();
        return ResponseEntity.ok(tenants);
    }

    @PostMapping("/tenant/{id}/subscription")
    public ResponseEntity<?> updateSubscription(@PathVariable UUID id, @RequestBody SubscriptionUpdateDto dto) {
        User currentUser = getCurrentUser();
        if (!currentUser.getRole().equals("SUPER_ADMIN")) return ResponseEntity.status(403).build();

        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant no encontrado"));

        // 1. CAMBIO DE PLAN (Si viene en el DTO)
        if (dto.getNewPlanName() != null && !dto.getNewPlanName().isEmpty()) {
            Plan newPlan = planRepository.findByName(dto.getNewPlanName());
            if (newPlan != null) {
                tenant.setPlan(newPlan);
                // Opcional: Si pasa a pagado, ya no es demo account
                 tenant.setIsDemoAccount(false);
            }
        }

        // 2. CÁLCULO DE TIEMPO (ACUMULATIVO)
        if (dto.getMonthsToAdd() != null && dto.getMonthsToAdd() > 0) {
            LocalDateTime now = LocalDateTime.now();

            // Buscamos cuál es la fecha más lejana que tiene el cliente actualmente
            LocalDateTime baseDate = now; // Por defecto hoy

            // Si tiene suscripción activa futura, usamos esa
            if (tenant.getSubscriptionEndDate() != null && tenant.getSubscriptionEndDate().isAfter(baseDate)) {
                baseDate = tenant.getSubscriptionEndDate();
            }
            // Si no tiene suscripción pero tiene demo activa futura, usamos esa (respetamos los días de demo)
            else if (tenant.getDemoExpiresAt() != null && tenant.getDemoExpiresAt().isAfter(baseDate)) {
                baseDate = tenant.getDemoExpiresAt();
            }

            // Sumamos el tiempo a la fecha base calculada
            tenant.setSubscriptionEndDate(baseDate.plusMonths(dto.getMonthsToAdd()));
            tenant.setPlanStatus("ACTIVE");
        }

        // 3. Otros datos
        if (dto.getExtraCashiers() != null) tenant.setMaxCashiersExtra(dto.getExtraCashiers());
        if (dto.getStatus() != null) tenant.setPlanStatus(dto.getStatus());

        tenantRepository.save(tenant);
        return ResponseEntity.ok().build();
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName()).orElseThrow();
    }
}