package com.saaspos.api.repository;

import com.saaspos.api.model.ecommerce.ShopConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ShopConfigRepository extends JpaRepository<ShopConfig, UUID> {
    // Buscar configuración por el SLUG (ej: 'mi-tienda')
    Optional<ShopConfig> findByUrlSlug(String urlSlug);

    // Para validar que no se repitan al crear
    boolean existsByUrlSlug(String urlSlug);

    // Para buscar por tenant interno
    Optional<ShopConfig> findByTenantId(UUID tenantId);
}