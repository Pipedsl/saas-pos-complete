package com.saaspos.api.dto;

import lombok.Data;

@Data
public class SubscriptionUpdateDto {
    private Integer monthsToAdd; // 1, 3, 6, 12 (0 si solo editas límites)
    private Integer extraCashiers; // Cantidad total extra
    private String status; // ACTIVE, SUSPENDED
    private String newPlanName;
}