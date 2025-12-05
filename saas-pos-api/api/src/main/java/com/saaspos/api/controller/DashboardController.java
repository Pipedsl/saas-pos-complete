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
@CrossOrigin(origins = "*")
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

        // 1. Configurar Zonas Horarias
        ZoneId chileZone = ZoneId.of("America/Santiago");
        ZoneId utcZone = ZoneId.of("UTC");

        // 2. Calcular rangos de tiempo exactos
        ZonedDateTime nowChile = ZonedDateTime.now(chileZone);

        // Inicio del día en Chile (00:00:00)
        ZonedDateTime startChile = nowChile.toLocalDate().atStartOfDay(chileZone);
        // Fin del día en Chile (23:59:59)
        ZonedDateTime endChile = nowChile.toLocalDate().atTime(LocalTime.MAX).atZone(chileZone);


        LocalDateTime startUTC = startChile.withZoneSameInstant(utcZone).toLocalDateTime();
        LocalDateTime endUTC = endChile.withZoneSameInstant(utcZone).toLocalDateTime();


        // Debug para consola
        System.out.println("--- DASHBOARD MIXTO ---");
        System.out.println("Rango UTC calculado: " + startUTC + " a " + endUTC);

        // 3. Consultas
        LocalDate todayChile = nowChile.toLocalDate();

        BigDecimal totalSales = saleRepository.sumTotalSalesByDate(tenantId, todayChile);
        long transactions = saleRepository.countTransactionsByDate(tenantId, todayChile);
        long lowStock = productRepository.countLowStock(tenantId);

        // Aquí sí usamos las variables UTC que acabamos de definir
        BigDecimal totalProfit = saleRepository.calculateProfitByRange(tenantId, startUTC, endUTC);

        return ResponseEntity.ok(new DashboardStatsDto(totalSales, transactions, lowStock, totalProfit));
    }

    private UUID getCurrentTenantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        return user.getTenant().getId();
    }
}