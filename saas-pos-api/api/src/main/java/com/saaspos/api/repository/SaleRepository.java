package com.saaspos.api.repository;

import com.saaspos.api.dto.ChartDataDto;
import com.saaspos.api.dto.ProductRankingDto;
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
    boolean existsBySaleNumber(String saleNumber);
    // 1. SUMA TOTAL (Por Rango)
    @Query(value = "SELECT COALESCE(SUM(total_amount), 0) FROM sales WHERE tenant_id = :tenantId " +
            "AND CAST(created_at AT TIME ZONE 'America/Santiago' AS DATE) BETWEEN :startDate AND :endDate", nativeQuery = true)
    BigDecimal sumTotalSalesByDateRange(@Param("tenantId") UUID tenantId,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);

    // 2. CONTEO TICKETS (Por Rango)
    @Query(value = "SELECT COUNT(*) FROM sales WHERE tenant_id = :tenantId " +
            "AND CAST(created_at AT TIME ZONE 'America/Santiago' AS DATE) BETWEEN :startDate AND :endDate", nativeQuery = true)
    long countTransactionsByDateRange(@Param("tenantId") UUID tenantId,
                                      @Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);

    // 3. UTILIDAD (Por Rango)
    @Query(value = """
        SELECT COALESCE(SUM(
            (si.net_price_at_sale - si.cost_price_at_sale) * si.quantity
        ), 0)
        FROM sale_items si
        JOIN sales s ON si.sale_id = s.id
        WHERE s.tenant_id = :tenantId
          AND s.status = 'COMPLETED'
          AND CAST(s.created_at AT TIME ZONE 'America/Santiago' AS DATE) BETWEEN :startDate AND :endDate
    """, nativeQuery = true)
    BigDecimal calculateRealProfitByDateRange(@Param("tenantId") UUID tenantId,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    // 4. LISTA GENERAL (Ordenada por fecha)
    List<Sale> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    @Query("SELECT DISTINCT s FROM Sale s " +
            "LEFT JOIN FETCH s.items i " +
            "LEFT JOIN FETCH i.product p " +
            "WHERE s.id = :id")
    Optional<Sale> findByIdWithItems(@Param("id") UUID id);

    // 5. DETALLE DE VENTAS POR RANGO (Para el modal de "Ventas Hoy" / "Tickets")
    // Trae las ventas completas para mostrar en la tabla de detalle
    @Query(value = "SELECT * FROM sales s WHERE s.tenant_id = :tenantId " +
            "AND CAST(s.created_at AT TIME ZONE 'America/Santiago' AS DATE) BETWEEN :startDate AND :endDate " +
            "ORDER BY s.created_at DESC", nativeQuery = true)
    List<Sale> findSalesByDateRange(@Param("tenantId") UUID tenantId,
                                    @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);

    // 6. RANKING DE PRODUCTOS (Más vendidos, Menos vendidos, Por categoría)
    // Agrupa por producto y suma cantidades y montos
    @Query(value = """
        SELECT 
            p.name as productName,
            p.sku as sku,
            c.name as categoryName,
            SUM(si.quantity) as totalQuantitySold,
            SUM(si.total) as totalRevenue
        FROM sale_items si
        JOIN sales s ON si.sale_id = s.id
        JOIN products p ON si.product_id = p.id
        LEFT JOIN categories c ON p.category_id = c.id
        WHERE s.tenant_id = :tenantId
          AND s.status = 'COMPLETED'
          AND CAST(s.created_at AT TIME ZONE 'America/Santiago' AS DATE) BETWEEN :startDate AND :endDate
        GROUP BY p.id, p.name, p.sku, c.name
        ORDER BY totalQuantitySold DESC
    """, nativeQuery = true)
    List<ProductRankingDto> getProductRankingByDateRange(@Param("tenantId") UUID tenantId,
                                                         @Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate);

    // 7. DATOS PARA GRÁFICO (Agrupados por Día)
    // Se usa para: Semana y Mes
    @Query(value = """
        SELECT 
            to_char(created_at AT TIME ZONE 'America/Santiago', 'DD/MM') as label, 
            SUM(total_amount) as value 
        FROM sales 
        WHERE tenant_id = :tenantId 
          AND status = 'COMPLETED'
          AND CAST(created_at AT TIME ZONE 'America/Santiago' AS DATE) BETWEEN :startDate AND :endDate
        GROUP BY 1
        ORDER BY MIN(created_at) ASC
    """, nativeQuery = true)
    List<ChartDataDto> getSalesChartByDay(@Param("tenantId") UUID tenantId,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    // 8. DATOS PARA GRÁFICO (Agrupados por Mes)
    // Se usa para: Año
    @Query(value = """
        SELECT 
            to_char(created_at AT TIME ZONE 'America/Santiago', 'MM/YYYY') as label, 
            SUM(total_amount) as value 
        FROM sales 
        WHERE tenant_id = :tenantId 
          AND status = 'COMPLETED'
          AND CAST(created_at AT TIME ZONE 'America/Santiago' AS DATE) BETWEEN :startDate AND :endDate
        GROUP BY 1
        ORDER BY MIN(created_at) ASC
    """, nativeQuery = true)
    List<ChartDataDto> getSalesChartByMonth(@Param("tenantId") UUID tenantId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    // 9. MÁS VENDIDOS (Con filtro Categoría)
    @Query(value = """
        SELECT 
            p.name as productName,
            p.sku as sku,
            c.name as categoryName,
            SUM(si.quantity) as totalQuantitySold,
            SUM(si.total) as totalRevenue
        FROM sale_items si
        JOIN sales s ON si.sale_id = s.id
        JOIN products p ON si.product_id = p.id
        LEFT JOIN categories c ON p.category_id = c.id
        WHERE s.tenant_id = :tenantId
          AND s.status = 'COMPLETED'
          AND CAST(s.created_at AT TIME ZONE 'America/Santiago' AS DATE) BETWEEN :startDate AND :endDate
          AND (:categoryId IS NULL OR p.category_id = :categoryId)
        GROUP BY p.id, p.name, p.sku, c.name
        ORDER BY totalQuantitySold DESC
    """, nativeQuery = true)
    List<ProductRankingDto> findMostSold(@Param("tenantId") UUID tenantId,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate,
                                         @Param("categoryId") UUID categoryId);

    // 10. MENOS VENDIDOS (Con filtro Categoría)
    @Query(value = """
        SELECT 
            p.name as productName,
            p.sku as sku,
            c.name as categoryName,
            SUM(si.quantity) as totalQuantitySold,
            SUM(si.total) as totalRevenue
        FROM sale_items si
        JOIN sales s ON si.sale_id = s.id
        JOIN products p ON si.product_id = p.id
        LEFT JOIN categories c ON p.category_id = c.id
        WHERE s.tenant_id = :tenantId
          AND s.status = 'COMPLETED'
          AND CAST(s.created_at AT TIME ZONE 'America/Santiago' AS DATE) BETWEEN :startDate AND :endDate
          AND (:categoryId IS NULL OR p.category_id = :categoryId)
        GROUP BY p.id, p.name, p.sku, c.name
        ORDER BY totalQuantitySold ASC
    """, nativeQuery = true)
    List<ProductRankingDto> findLeastSold(@Param("tenantId") UUID tenantId,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate,
                                          @Param("categoryId") UUID categoryId);

    // 11. NO VENDIDOS (Productos activos sin movimiento en el periodo)
    @Query(value = """
        SELECT 
            p.name as productName,
            p.sku as sku,
            c.name as categoryName,
            0 as totalQuantitySold,
            0 as totalRevenue
        FROM products p
        LEFT JOIN categories c ON p.category_id = c.id
        WHERE p.tenant_id = :tenantId
          AND p.is_active = true
          AND (:categoryId IS NULL OR p.category_id = :categoryId)
          AND p.id NOT IN (
              SELECT DISTINCT si.product_id
              FROM sale_items si
              JOIN sales s ON si.sale_id = s.id
              WHERE s.tenant_id = :tenantId
                AND s.status = 'COMPLETED'
                AND CAST(s.created_at AT TIME ZONE 'America/Santiago' AS DATE) BETWEEN :startDate AND :endDate
          )
        ORDER BY p.name ASC
    """, nativeQuery = true)
    List<ProductRankingDto> findUnsold(@Param("tenantId") UUID tenantId,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate,
                                       @Param("categoryId") UUID categoryId);
}