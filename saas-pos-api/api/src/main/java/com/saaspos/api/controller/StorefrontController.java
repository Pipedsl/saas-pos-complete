package com.saaspos.api.controller;

import com.saaspos.api.dto.ecommerce.PublicProductDto;
import com.saaspos.api.dto.ecommerce.PublicShopDto;
import com.saaspos.api.dto.ecommerce.WebOrderRequest;
import com.saaspos.api.model.ecommerce.WebOrder;
import com.saaspos.api.service.StorefrontService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/store") // <--- OJO CON ESTA RUTA
public class StorefrontController {

    private final StorefrontService storefrontService;

    public StorefrontController(StorefrontService storefrontService) {
        this.storefrontService = storefrontService;
    }

    @GetMapping("/{slug}")
    public ResponseEntity<PublicShopDto> getShopInfo(@PathVariable String slug) {
        return ResponseEntity.ok(storefrontService.getShopInfo(slug));
    }

    @GetMapping("/{slug}/products")
    public ResponseEntity<List<PublicProductDto>> getShopProducts(@PathVariable String slug) {
        return ResponseEntity.ok(storefrontService.getShopProducts(slug));
    }

    @PostMapping("/{slug}/orders")
    public ResponseEntity<?> createOrder(@PathVariable String slug, @RequestBody WebOrderRequest request) {
        try {
            WebOrder createdOrder = storefrontService.createOrder(slug, request);
            // Retornamos solo el ID o un mensaje de éxito simple
            return ResponseEntity.ok().body(java.util.Map.of(
                    "orderId", createdOrder.getId(),
                    "orderNumber", createdOrder.getOrderNumber(),
                    "message", "Pedido creado exitosamente"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }
}