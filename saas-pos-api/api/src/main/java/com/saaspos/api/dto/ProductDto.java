package com.saaspos.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
public class ProductDto {
    private UUID id;

    @NotBlank(message = "El SKU es obligatorio")
    private String sku;

    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    private String description;

    @NotNull(message = "El precio es obligatorio")
    @Min(value = 0, message = "El precio no puede ser negativo")
    private BigDecimal priceNeto;

    private String measurementUnit;

    private BigDecimal taxPercent;
    private BigDecimal stockCurrent;
    private BigDecimal stockMin;

    //Aqui recibimos el JSON dinámico del frontend
    private Map<String, Object> attributes;

    private UUID categoryId; // <--- Nuevo: Recibimos el ID
    private String categoryName; // <--- Nuevo: Devolvemos el nombre para mostrar en la tabla

    private UUID supplierId;       // <--- Nuevo
    private String supplierName; // Salida (Output para la tabla)
    private BigDecimal costPrice;  // <--- Nuevo
    private BigDecimal marginPercent; // <--- Nuevo (Ej: 30.0 para 30%)

}
