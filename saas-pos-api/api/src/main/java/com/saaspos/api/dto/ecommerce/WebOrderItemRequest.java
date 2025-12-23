package com.saaspos.api.dto.ecommerce;

import lombok.Data;
import java.util.UUID;

@Data
public class WebOrderItemRequest {
    private UUID productId;
    private int quantity;
}