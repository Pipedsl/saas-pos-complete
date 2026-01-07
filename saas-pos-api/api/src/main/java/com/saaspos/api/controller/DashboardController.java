package com.saaspos.api.controller;

import com.saaspos.api.dto.ChartDataDto;
import com.saaspos.api.dto.DashboardStatsDto;
import com.saaspos.api.dto.ProductRankingDto;
import com.saaspos.api.model.Sale;
import com.saaspos.api.model.User;
import com.saaspos.api.repository.ProductRepository;
import com.saaspos.api.repository.SaleRepository;
import com.saaspos.api.repository.UserRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public DashboardController(SaleRepository saleRepository, ProductRepository productRepository, UserRepository userRepository) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> getStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        UUID tenantId = getCurrentTenantId();

        // Si no envían fechas, usamos HOY por defecto
        if (startDate == null || endDate == null) {
            ZoneId chileZone = ZoneId.of("America/Santiago");
            startDate = LocalDate.now(chileZone);
            endDate = LocalDate.now(chileZone);
        }

        BigDecimal totalSales = saleRepository.sumTotalSalesByDateRange(tenantId, startDate, endDate);
        long transactions = saleRepository.countTransactionsByDateRange(tenantId, startDate, endDate);
        long lowStock = productRepository.countLowStock(tenantId); // Este no depende de fechas
        BigDecimal totalProfit = saleRepository.calculateRealProfitByDateRange(tenantId, startDate, endDate);

        return ResponseEntity.ok(new DashboardStatsDto(totalSales, transactions, lowStock, totalProfit));
    }

    // 2. NUEVO: DETALLE DE VENTAS (Para al hacer clic en las tarjetas de Ventas/Tickets/Utilidad)
    @GetMapping("/sales-detail")
    public ResponseEntity<List<Sale>> getSalesDetail(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        UUID tenantId = getCurrentTenantId();
        List<Sale> sales = saleRepository.findSalesByDateRange(tenantId, startDate, endDate);
        return ResponseEntity.ok(sales);
    }

    // 3. NUEVO: RANKING DE PRODUCTOS
    @GetMapping("/product-ranking")
    public ResponseEntity<List<ProductRankingDto>> getProductRanking(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) UUID categoryId, // <--- NUEVO
            @RequestParam(defaultValue = "MOST_SOLD") String type // MOST_SOLD, LEAST_SOLD, UNSOLD
    ) {

        UUID tenantId = getCurrentTenantId();
        List<ProductRankingDto> ranking;

        switch (type) {
            case "LEAST_SOLD":
                ranking = saleRepository.findLeastSold(tenantId, startDate, endDate, categoryId);
                break;
            case "UNSOLD":
                ranking = saleRepository.findUnsold(tenantId, startDate, endDate, categoryId);
                break;
            case "MOST_SOLD":
            default:
                ranking = saleRepository.findMostSold(tenantId, startDate, endDate, categoryId);
                break;
        }

        return ResponseEntity.ok(ranking);
    }

    // 4. NUEVO: DATOS PARA GRÁFICOS
    @GetMapping("/chart-data")
    public ResponseEntity<List<ChartDataDto>> getChartData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String groupBy // "DAY" o "MONTH"
    ) {
        UUID tenantId = getCurrentTenantId();
        List<ChartDataDto> data;

        if ("MONTH".equals(groupBy)) {
            data = saleRepository.getSalesChartByMonth(tenantId, startDate, endDate);
        } else {
            // Por defecto agrupar por día (para Semana y Mes)
            data = saleRepository.getSalesChartByDay(tenantId, startDate, endDate);
        }

        return ResponseEntity.ok(data);
    }

    private UUID getCurrentTenantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        return user.getTenant().getId();
    }
}