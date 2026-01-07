package com.saaspos.api.dto;

public interface ChartDataDto {
    String getLabel(); // Ej: "05/01" o "Enero"
    Double getValue(); // Ej: 15000.00
}