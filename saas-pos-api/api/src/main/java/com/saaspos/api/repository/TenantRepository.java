package com.saaspos.api.repository;

import com.saaspos.api.dto.TenantSummaryDto;
import com.saaspos.api.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    // Metodo magico para buscar por rut
    Optional<Tenant> findByRut(String rut);

    @Query("""
        SELECT new com.saaspos.api.dto.TenantSummaryDto(
            t.id,
            t.companyName,
            u.fullName,
            u.email,
            p.name,
            t.isActive,
            t.createdAt,
            t.demoExpiresAt,
            'System'
        )
        FROM Tenant t
        JOIN t.plan p
        JOIN User u ON u.tenant.id = t.id
        WHERE u.role = 'TENANT_ADMIN'
        ORDER BY t.createdAt DESC
    """)
    List<TenantSummaryDto> findAllTenantSummaries();
}
