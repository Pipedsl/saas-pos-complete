package com.saaspos.api.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class SaleRequest {
    // Lista simple de productos y cantidades
    private List<SaleItemRequest> items;
    private BigDecimal totalAmount; // Para validar backend vs frontend

    @Data
    public static class SaleItemRequest {
        private UUID productId;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
    }
}