package com.saaspos.api.controller;

import com.saaspos.api.model.Tenant;
import com.saaspos.api.model.User;
import com.saaspos.api.repository.TenantRepository;
import com.saaspos.api.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings") // Nueva ruta para configuraciones
@CrossOrigin(origins = "*")
public class TenantController {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    public TenantController(TenantRepository tenantRepository, UserRepository userRepository) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
    }

    // Obtener configuración
    @GetMapping
    public ResponseEntity<Map<String, String>> getSettings() {
        User user = getCurrentUser();
        return ResponseEntity.ok(user.getTenant().getSettings());
    }

    // Guardar configuración (Solo Admin)
    @PostMapping
    public ResponseEntity<?> updateSettings(@RequestBody Map<String, String> newSettings) {
        User user = getCurrentUser();

        if (!user.getRole().contains("ADMIN")) {
            return ResponseEntity.status(403).body("Solo el administrador configura la tienda.");
        }

        Tenant tenant = user.getTenant();
        tenant.setSettings(newSettings); // Reemplaza o fusiona según lógica
        tenantRepository.save(tenant);

        return ResponseEntity.ok(tenant.getSettings());
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName()).orElseThrow();
    }
}