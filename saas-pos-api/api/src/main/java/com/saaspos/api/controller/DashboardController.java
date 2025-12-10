package com.saaspos.api.controller;

import com.saaspos.api.dto.DashboardStatsDto;
import com.saaspos.api.model.User;
import com.saaspos.api.repository.ProductRepository;
import com.saaspos.api.repository.SaleRepository;
import com.saaspos.api.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
    public ResponseEntity<DashboardStatsDto> getStats() {
        UUID tenantId = getCurrentTenantId();

        // 1. Obtener Fecha Hoy en Chile
        ZoneId chileZone = ZoneId.of("America/Santiago");
        LocalDate todayChile = LocalDate.now(chileZone);

        System.out.println("--- DASHBOARD NATIVO ---");
        System.out.println("Tenant: " + tenantId);
        System.out.println("Fecha Chile: " + todayChile);

        // 2. Ejecutar Consultas
        BigDecimal totalSales = saleRepository.sumTotalSalesByDate(tenantId, todayChile);
        long transactions = saleRepository.countTransactionsByDate(tenantId, todayChile);
        long lowStock = productRepository.countLowStock(tenantId);

        // 3. Utilidad Real (Usando la nueva query optimizada)
        BigDecimal totalProfit = saleRepository.calculateRealProfit(tenantId, todayChile);

        return ResponseEntity.ok(new DashboardStatsDto(totalSales, transactions, lowStock, totalProfit));
    }

    private UUID getCurrentTenantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        return user.getTenant().getId();
    }
}