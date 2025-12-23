package com.saaspos.api.service;

import com.saaspos.api.dto.ecommerce.PublicProductDto;
import com.saaspos.api.dto.ecommerce.PublicShopDto;
import com.saaspos.api.dto.ecommerce.WebOrderItemRequest;
import com.saaspos.api.dto.ecommerce.WebOrderRequest;
import com.saaspos.api.model.Product;
import com.saaspos.api.model.ecommerce.ShopConfig;
import com.saaspos.api.model.ecommerce.WebOrder;
import com.saaspos.api.model.ecommerce.WebOrderItem;
import com.saaspos.api.repository.ProductRepository;
import com.saaspos.api.repository.ShopConfigRepository;
import com.saaspos.api.repository.WebOrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StorefrontService {

    private final ShopConfigRepository shopConfigRepository;
    private final ProductRepository productRepository;
    private final WebOrderRepository webOrderRepository;

    public StorefrontService(ShopConfigRepository shopConfigRepository, ProductRepository productRepository, WebOrderRepository webOrderRepository) {
        this.shopConfigRepository = shopConfigRepository;
        this.productRepository = productRepository;
        this.webOrderRepository = webOrderRepository;
    }

    // 1. Obtener Info de la Tienda
    public PublicShopDto getShopInfo(String slug) {
        ShopConfig config = shopConfigRepository.findByUrlSlug(slug)
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada"));

        if (!config.isActive()) {
            throw new RuntimeException("Esta tienda está cerrada temporalmente");
        }

        PublicShopDto dto = new PublicShopDto();
        dto.setShopName(config.getShopName());
        dto.setUrlSlug(config.getUrlSlug());
        dto.setLogoUrl(config.getLogoUrl());
        dto.setBannerUrl(config.getBannerUrl());
        dto.setPrimaryColor(config.getPrimaryColor());
        dto.setPaymentMethods(config.getPaymentMethods());
        dto.setShippingMethods(config.getShippingMethods());
        return dto;
    }

    // 2. Obtener Productos Públicos
    public List<PublicProductDto> getShopProducts(String slug) {
        // Primero buscamos la config para saber el Tenant ID
        ShopConfig config = shopConfigRepository.findByUrlSlug(slug)
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada"));

        // Busamos productos de ese tenant que sean públicos
        List<Product> products = productRepository.findByTenantIdAndIsPublicTrue(config.getTenantId());

        return products.stream().map(this::mapToPublicDto).collect(Collectors.toList());
    }

    @Transactional // Importante: Todo o nada
    public WebOrder createOrder(String slug, WebOrderRequest request) {
        // 1. Identificar la tienda
        ShopConfig config = shopConfigRepository.findByUrlSlug(slug)
                .orElseThrow(() -> new RuntimeException("Tienda no encontrada"));

        // 2. Crear la cabecera del pedido
        WebOrder order = new WebOrder();
        order.setTenantId(config.getTenantId());
        order.setOrderNumber("WEB-" + System.currentTimeMillis() % 10000); // Generador simple temporal
        order.setCustomerName(request.getCustomerName());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setShippingAddress(request.getCustomerAddress());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setShippingMethod(request.getDeliveryMethod());

        order.setStatus("PENDING"); // Estado inicial (Reserva)
        // La reserva expira en X minutos (configurado en DB o 30 por defecto)
        order.setExpiresAt(LocalDateTime.now().plusMinutes(config.getReservationMinutes() != null ? config.getReservationMinutes() : 30));

        BigDecimal totalOrder = BigDecimal.ZERO;
        List<WebOrderItem> orderItems = new ArrayList<>();

        // 3. Procesar Items y STOCK
        for (WebOrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + itemReq.getProductId()));

            // Validar que el producto sea de este tenant
            if (!product.getTenantId().equals(config.getTenantId())) {
                throw new RuntimeException("Producto inválido");
            }

            // --- VALIDACIÓN DE STOCK CRÍTICA ---
            BigDecimal qty = new BigDecimal(itemReq.getQuantity());
            if (product.getStockCurrent().compareTo(qty) < 0) {
                throw new RuntimeException("Stock insuficiente para: " + product.getName());
            }

            // --- DESCUENTO DE STOCK (RESERVA) ---
            // Restamos ahora. Si el pedido expira, un cron job debería devolverlo (lo veremos luego)
            product.setStockCurrent(product.getStockCurrent().subtract(qty));
            productRepository.save(product);

            // Crear Detalle
            WebOrderItem detail = new WebOrderItem();
            detail.setWebOrder(order);
            detail.setProduct(product);
            detail.setQuantity(qty);

            // Snapshot de precios (al momento de la compra)
            BigDecimal finalPrice = product.getPublicPrice() != null ? product.getPublicPrice() : calculatePriceWithTax(product);
            detail.setUnitPriceAtMoment(finalPrice);
            detail.setCostPriceAtMoment(product.getCostPrice());
            detail.setProductNameSnapshot(product.getName());
            detail.setSkuSnapshot(product.getSku());

            // Subtotal línea
            BigDecimal subtotal = finalPrice.multiply(qty);
            detail.setSubtotal(subtotal);

            orderItems.add(detail);
            totalOrder = totalOrder.add(subtotal);
        }

        order.setItems(orderItems);
        order.setFinalTotal(totalOrder);
        order.setTotalItems(new BigDecimal(orderItems.size()));

        return webOrderRepository.save(order);
    }

    // Auxiliar para calcular precio normal con IVA si no hay precio público especial
    private BigDecimal calculatePriceWithTax(Product p) {
        BigDecimal taxFactor = BigDecimal.ONE.add(p.getTaxPercent().divide(new BigDecimal("100")));
        return p.getPriceNeto().multiply(taxFactor); // Ajustar redondeo si es necesario
    }

    private PublicProductDto mapToPublicDto(Product p) {
        PublicProductDto dto = new PublicProductDto();
        dto.setId(p.getId());
        dto.setSku(p.getSku());
        dto.setName(p.getName());
        dto.setDescription(p.getDescriptionWeb() != null ? p.getDescriptionWeb() : p.getDescription());
        dto.setImageUrl(p.getImageUrl());

        // Lógica de Precio Público:
        // Si tiene precio especial web, úsalo. Si no, calcula el precio normal con IVA.
        BigDecimal finalPrice;
        if (p.getPublicPrice() != null && p.getPublicPrice().compareTo(BigDecimal.ZERO) > 0) {
            finalPrice = p.getPublicPrice();
        } else {
            // Precio Normal (Neto * 1.19)
            BigDecimal taxFactor = BigDecimal.ONE.add(p.getTaxPercent().divide(new BigDecimal("100")));
            finalPrice = p.getPriceNeto().multiply(taxFactor);
        }
        dto.setPrice(finalPrice.setScale(0, RoundingMode.HALF_UP));

        dto.setStockCurrent(p.getStockCurrent());
        dto.setLowStock(p.getStockCurrent().compareTo(p.getStockMin()) <= 0);

        if (p.getCategory() != null) {
            dto.setCategoryName(p.getCategory().getName());
        }

        return dto;
    }
}