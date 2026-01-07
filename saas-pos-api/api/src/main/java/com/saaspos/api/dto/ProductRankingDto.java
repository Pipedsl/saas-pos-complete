package com.saaspos.api.dto;

import java.math.BigDecimal;

// Usamos una interfaz para que Spring Data JPA proyecte los resultados nativos automáticamente
public interface ProductRankingDto {
    String getProductName();
    String getSku();
    String getCategoryName();
    BigDecimal getTotalQuantitySold(); // Cantidad vendida
    BigDecimal getTotalRevenue();      // Dinero generado (Ventas)
}