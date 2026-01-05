package com.saaspos.api.repository;

import com.saaspos.api.model.ecommerce.WebOrder;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebOrderRepository extends JpaRepository<WebOrder, UUID> {
    // Método útil: Buscar órdenes por Tenant (para que la empresa vea sus ventas)
    List<WebOrder> findByTenantId(UUID tenantId);

    // Método útil: Buscar órdenes de un cliente específico por su email
    List<WebOrder> findByCustomerEmail(String customerEmail);
    // Método crítico para el Hard Delete
    @Modifying
    @Transactional
    @Query("DELETE FROM SaleItem s WHERE s.product.id = :productId")
    void deleteByProductId(UUID productId);

    Optional<WebOrder> findByOrderNumberAndTenantId(String orderNumber, UUID tenantId);

    List<WebOrder> findByStatusAndExpiresAtBefore(String status, LocalDateTime now);
}
