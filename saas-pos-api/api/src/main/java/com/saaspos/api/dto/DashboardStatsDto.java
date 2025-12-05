package com.saaspos.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class DashboardStatsDto {
    private BigDecimal totalSalesToday; // Dinero hoy
    private long totalTransactionsToday; // Tickets hoy
    private long lowStockCount;          // Productos por agotar
    private BigDecimal totalProfitToday;
}