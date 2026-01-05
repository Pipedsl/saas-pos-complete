package com.saaspos.api.controller;

import com.saaspos.api.model.User;
import com.saaspos.api.model.ecommerce.ShopConfig;
import com.saaspos.api.repository.ShopConfigRepository;
import com.saaspos.api.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/shop-config")
public class AdminShopConfigController {

    private final ShopConfigRepository shopConfigRepository;
    private final UserRepository userRepository;

    public AdminShopConfigController(ShopConfigRepository shopConfigRepository, UserRepository userRepository) {
        this.shopConfigRepository = shopConfigRepository;
        this.userRepository = userRepository;
    }

    // 1. Obtener MI configuración
    @GetMapping
    public ResponseEntity<ShopConfig> getMyConfig() {
        UUID tenantId = getCurrentTenantId();

        ShopConfig config = shopConfigRepository.findByTenantId(tenantId)
                .orElseGet(() -> createDefaultConfig(tenantId)); // Si no existe, crea una por defecto

        return ResponseEntity.ok(config);
    }

    // 2. Actualizar configuración (Logo, Color, Métodos)
    @PutMapping
    public ResponseEntity<ShopConfig> updateConfig(@RequestBody ShopConfig dto) {
        UUID tenantId = getCurrentTenantId();

        ShopConfig config = shopConfigRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new RuntimeException("Configuración no encontrada"));

        // Actualizamos solo lo que el dueño puede cambiar
        config.setShopName(dto.getShopName());
        config.setUrlSlug(dto.getUrlSlug());
        config.setPrimaryColor(dto.getPrimaryColor()); // Color Picker
        config.setLogoUrl(dto.getLogoUrl());           // URL de Firebase
        config.setBannerUrl(dto.getBannerUrl());

        config.setContactPhone(dto.getContactPhone());
        config.setReservationMinutes(dto.getReservationMinutes() != null ? dto.getReservationMinutes() : 60);

        config.setRecommendedCourier(dto.getRecommendedCourier());
        config.setDispatchDays(dto.getDispatchDays());

        // Métodos de pago y envío (JSONB)
        // El front enviará un JSON tipo: {"cash": true, "transfer": false}
        config.setPaymentMethods(dto.getPaymentMethods());
        config.setShippingMethods(dto.getShippingMethods());

        // Catálogo activo o mantenimiento
        config.setActive(dto.isActive());

        ShopConfig saved = shopConfigRepository.save(config);
        return ResponseEntity.ok(saved);
    }

    private ShopConfig createDefaultConfig(UUID tenantId) {
        ShopConfig config = new ShopConfig();
        config.setTenantId(tenantId);
        config.setUrlSlug("tienda-" + UUID.randomUUID().toString().substring(0, 8)); // Slug temporal
        config.setShopName("Mi Nueva Tienda");
        config.setActive(true); // Activa por defecto al crear
        return shopConfigRepository.save(config);
    }

    private UUID getCurrentTenantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return user.getTenant().getId();
    }
}