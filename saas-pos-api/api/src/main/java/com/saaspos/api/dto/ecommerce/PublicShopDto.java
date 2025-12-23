package com.saaspos.api.dto.ecommerce;

import lombok.Data;
import java.util.Map;

@Data
public class PublicShopDto {
    private String shopName;
    private String logoUrl;
    private String bannerUrl;
    private String primaryColor;
    private String urlSlug;

    // Solo enviamos configuraciones necesarias para la UI (no secretos)
    private Map<String, Object> paymentMethods;
    private Map<String, Object> shippingMethods;
}