package com.saaspos.api.repository;

import com.saaspos.api.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import java.math.BigDecimal;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// <Entidad, TipoDeID>
public interface SaleRepository extends JpaRepository<Sale, UUID> {
    // 1. SUMA TOTAL (SQL Nativo)
    // COALESCE(..., 0) sirve para que si no hay ventas devuelva 0 en vez de NULL
    @Query(value = "SELECT COALESCE(SUM(total_amount), 0) FROM sales WHERE tenant_id = :tenantId AND CAST(created_at AT TIME ZONE 'America/Santiago' AS DATE) = :date", nativeQuery = true)
    BigDecimal sumTotalSalesByDate(@Param("tenantId") UUID tenantId, @Param("date") LocalDate date);

    // 2. CONTEO TICKETS (SQL Nativo)
    @Query(value = "SELECT COUNT(*) FROM sales WHERE tenant_id = :tenantId AND CAST(created_at AT TIME ZONE 'America/Santiago' AS DATE) = :date", nativeQuery = true)
    long countTransactionsByDate(@Param("tenantId") UUID tenantId, @Param("date") LocalDate date);

    @Query(value = """
        SELECT COALESCE(SUM(
            (si.net_price_at_sale - si.cost_price_at_sale) * si.quantity
        ), 0)
        FROM sale_items si
        JOIN sales s ON si.sale_id = s.id
        WHERE s.tenant_id = :tenantId 
          -- Filtramos por fecha usando la conversión de zona horaria de la BD (más robusto si el server está en UTC)
          AND CAST(s.created_at AT TIME ZONE 'America/Santiago' AS DATE) = :date
          AND s.status = 'COMPLETED'
    """, nativeQuery = true)
    BigDecimal calculateRealProfit(
            @Param("tenantId") UUID tenantId,
            @Param("date") LocalDate date
    );

}