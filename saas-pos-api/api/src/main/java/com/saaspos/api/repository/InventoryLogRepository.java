package com.saaspos.api.repository;

import com.saaspos.api.model.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface InventoryLogRepository extends JpaRepository<InventoryLog, UUID> {

    // 1. Filtro Básico: Todo el historial entre dos fechas
    List<InventoryLog> findByTenantIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            UUID tenantId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    // 2. Filtro Avanzado: Historial de una Categoría Específica
    // Hacemos un JOIN implícito con la tabla Product para filtrar por category_id
    @Query("SELECT log FROM InventoryLog log, Product p " +
            "WHERE log.productId = p.id " +
            "AND log.tenantId = :tenantId " +
            "AND p.category.id = :categoryId " +
            "AND log.createdAt BETWEEN :startDate AND :endDate " +
            "ORDER BY log.createdAt DESC")
    List<InventoryLog> findByCategoryAndDateRange(
            @Param("tenantId") UUID tenantId,
            @Param("categoryId") UUID categoryId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}