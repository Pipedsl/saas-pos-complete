package com.saaspos.api.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class SaleItemRequest {
    private UUID productId;
    private BigDecimal quantity;

    // Opcional: Si permites que el vendedor cambie el precio manualmente en el POS
    // private BigDecimal unitPrice;
}