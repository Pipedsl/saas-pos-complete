package com.saaspos.api.controller;

import com.saaspos.api.dto.TenantSummaryDto;
import com.saaspos.api.model.User;
import com.saaspos.api.repository.TenantRepository;
import com.saaspos.api.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class SuperAdminController {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    public SuperAdminController(TenantRepository tenantRepository, UserRepository userRepository) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
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

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName()).orElseThrow();
    }
}