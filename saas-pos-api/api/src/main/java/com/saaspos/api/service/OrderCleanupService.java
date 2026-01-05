package com.saaspos.api.service;

import com.saaspos.api.model.Product;
import com.saaspos.api.model.ecommerce.WebOrder;
import com.saaspos.api.model.ecommerce.WebOrderItem;
import com.saaspos.api.repository.ProductRepository;
import com.saaspos.api.repository.WebOrderRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderCleanupService {

    private final WebOrderRepository webOrderRepository;
    private final ProductRepository productRepository;

    public OrderCleanupService(WebOrderRepository webOrderRepository, ProductRepository productRepository) {
        this.webOrderRepository = webOrderRepository;
        this.productRepository = productRepository;
    }

    // Se ejecuta cada 60.000 ms = 1 minuto
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void releaseExpiredOrders() {
        // 1. Buscar pedidos PENDIENTES que ya vencieron (ExpiresAt < Ahora)
        LocalDateTime now = LocalDateTime.now();
        List<WebOrder> expiredOrders = webOrderRepository.findByStatusAndExpiresAtBefore("PENDING", now);

        if (!expiredOrders.isEmpty()) {
            System.out.println("--- 🧹 LIMPIEZA DE PEDIDOS: " + now + " ---");
            System.out.println("Se encontraron " + expiredOrders.size() + " pedidos vencidos.");
        }

        for (WebOrder order : expiredOrders) {
            System.out.println(">> Expirando orden: " + order.getOrderNumber());

            // 2. Devolver Stock al Inventario
            for (WebOrderItem item : order.getItems()) {
                Product product = item.getProduct();

                if (product != null) {
                    // Obtenemos producto actualizado
                    Product dbProduct = productRepository.findById(product.getId()).orElse(product);

                    // Sumamos la cantidad reservada de vuelta al stock
                    dbProduct.setStockCurrent(dbProduct.getStockCurrent().add(item.getQuantity()));
                    productRepository.save(dbProduct);

                    System.out.println("   + Stock liberado: " + item.getQuantity() + " unidades de '" + dbProduct.getName() + "'");
                }
            }

            // 3. Cambiar estado a EXPIRED
            order.setStatus("EXPIRED");
            order.setEditReason("Expirado automáticamente por tiempo de reserva");
            webOrderRepository.save(order);
        }
    }
}