package com.saaspos.api.dto.ecommerce;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class WebOrderItemRequest {
    private UUID productId;
    private int quantity;
    private BigDecimal customPrice;
}