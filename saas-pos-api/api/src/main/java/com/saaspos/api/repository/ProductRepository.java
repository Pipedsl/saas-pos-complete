package com.saaspos.api.repository;

import com.saaspos.api.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    //Buscar todos los productos de una empresa especifica
    List<Product> findByTenantId(UUID tenantId);

    //Buscar por SKU dentro de una empresa (el SKU puede repetirse en empresas distintas)
    boolean existsByTenantIdAndSku(UUID tenantId, String sku);

    long countByTenantId(UUID tenantId);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.tenantId = :tenantId AND p.stockCurrent <= p.stockMin")
    long countLowStock(@Param("tenantId") UUID tenantId);
}
