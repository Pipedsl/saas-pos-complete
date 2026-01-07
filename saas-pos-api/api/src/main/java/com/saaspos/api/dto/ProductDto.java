package com.saaspos.api.dto;

import jakarta.persistence.Column;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class ProductDto {
    private UUID id;

    private String sku;
    @NotBlank(message = "El nombre es obligatorio")
    private String name;
    private String description;
    private BigDecimal stockCurrent;
    private BigDecimal stockMin;
    private String measurementUnit;
    //Aqui recibimos el JSON dinámico del frontend
    private Map<String, Object> attributes;

    private UUID categoryId; // <--- Nuevo: Recibimos el ID
    private String categoryName; // <--- Nuevo: Devolvemos el nombre para mostrar en la tabla

    private UUID supplierId;       // <--- Nuevo
    private String supplierName; // Salida (Output para la tabla)

    private BigDecimal costPrice;      // Input
    private BigDecimal priceFinal;     // Input
    private Boolean isTaxIncluded;     // Input (Checkbox)

    private BigDecimal priceNeto;      // Output (Calculado)
    private BigDecimal taxPercent;     // Input/Output
    private BigDecimal marginPercent;  // Output (Calculado para mostrar ganancia)
    private String imageUrl;
    private Boolean isActive;
    private Boolean isPublic; // ¿Se muestra en la web?

    private String productType; // "STANDARD" o "BUNDLE"
    private List<BundleItemDto> bundleItems; // Lista de hijos

    @Data
    public static class BundleItemDto {
        private UUID componentId;
        private BigDecimal quantity;
        // Campos de lectura para el frontend
        private String componentName;
        private String componentSku;
    }



}
