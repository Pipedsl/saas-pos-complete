package com.saaspos.api.dto.ecommerce;

import lombok.Data;
import java.util.List;

@Data
public class WebOrderRequest {
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String customerRut;

    // Dirección desglosada
    private String region;
    private String commune;
    private String streetAndNumber;

    private String customerAddress; // Opcional (solo delivery)

    private String paymentMethod;   // CASH, TRANSFER
    private String deliveryMethod;  // PICKUP, DELIVERY
    private String courier;

    private String notes;
    private List<WebOrderItemRequest> items;
}