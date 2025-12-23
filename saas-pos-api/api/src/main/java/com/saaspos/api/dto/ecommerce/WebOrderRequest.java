package com.saaspos.api.dto.ecommerce;

import lombok.Data;
import java.util.List;

@Data
public class WebOrderRequest {
    private String customerName;
    private String customerPhone;
    private String customerAddress; // Opcional (solo delivery)

    private String paymentMethod;   // CASH, TRANSFER
    private String deliveryMethod;  // PICKUP, DELIVERY

    private List<WebOrderItemRequest> items;
}