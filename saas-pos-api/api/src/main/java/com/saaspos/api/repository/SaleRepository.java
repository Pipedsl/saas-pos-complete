package com.saaspos.api.repository;

import com.saaspos.api.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SaleRepository extends JpaRepository<Sale, UUID> {

    // 1. SUMA TOTAL (Para el Dashboard)
    @Query(value = "SELECT COALESCE(SUM(total_amount), 0) FROM sales WHERE tenant_id = :tenantId AND CAST(created_at AT TIME ZONE 'America/Santiago' AS DATE) = :date", nativeQuery = true)
    BigDecimal sumTotalSalesByDate(@Param("tenantId") UUID tenantId, @Param("date") LocalDate date);

    // 2. CONTEO TICKETS (Para el Dashboard)
    @Query(value = "SELECT COUNT(*) FROM sales WHERE tenant_id = :tenantId AND CAST(created_at AT TIME ZONE 'America/Santiago' AS DATE) = :date", nativeQuery = true)
    long countTransactionsByDate(@Param("tenantId") UUID tenantId, @Param("date") LocalDate date);

    // 3. UTILIDAD (Para el Dashboard)
    @Query(value = """
        SELECT COALESCE(SUM(
            (si.net_price_at_sale - si.cost_price_at_sale) * si.quantity
        ), 0)
        FROM sale_items si
        JOIN sales s ON si.sale_id = s.id
        WHERE s.tenant_id = :tenantId
          AND CAST(s.created_at AT TIME ZONE 'America/Santiago' AS DATE) = :date
          AND s.status = 'COMPLETED'
    """, nativeQuery = true)
    BigDecimal calculateRealProfit(@Param("tenantId") UUID tenantId, @Param("date") LocalDate date);

    // 4. LISTA GENERAL (Ordenada por fecha)
    List<Sale> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    @Query("SELECT DISTINCT s FROM Sale s " +
            "LEFT JOIN FETCH s.items i " +
            "LEFT JOIN FETCH i.product p " +
            "WHERE s.id = :id")
    Optional<Sale> findByIdWithItems(@Param("id") UUID id);
}