package com.saaspos.api.dto.ecommerce;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PublicProductDto {
    private UUID id;
    private String sku;
    private String name;
    private String description; // La descripción web
    private BigDecimal price;   // El precio final (public_price o price_neto + iva)
    private String imageUrl;
    private String categoryName;
    private BigDecimal stockCurrent; // Para validar en el front si queda poco
    private boolean isLowStock;      // Alerta visual
}