package com.saaspos.api.repository;

import com.saaspos.api.model.Product;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    //Buscar todos los productos de una empresa especifica
    List<Product> findByTenantId(UUID tenantId);

    //Buscar por SKU dentro de una empresa (el SKU puede repetirse en empresas distintas)
    boolean existsByTenantIdAndSku(UUID tenantId, String sku);

    long countByTenantId(UUID tenantId);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.tenantId = :tenantId AND p.stockCurrent <= p.stockMin")
    long countLowStock(@Param("tenantId") UUID tenantId);

    // Buscar solo productos marcados como públicos para un tenant específico
    List<Product> findByTenantIdAndIsPublicTrue(UUID tenantId);

    // Método personalizado para BORRADO FÍSICO (Hard Delete)
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM products WHERE id = :id", nativeQuery = true)
    void deleteHard(@Param("id") UUID id);

    // Busca un producto ignorando si está activo o no (SQL Nativo puro)
    @Query(value = "SELECT * FROM products WHERE id = :id", nativeQuery = true)
    Optional<Product> findAnyStatusById(@Param("id") UUID id);

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.isActive = true WHERE p.id = :id")
    void activateProduct(@Param("id") UUID id);

    @Query("""
        SELECT p FROM Product p
        LEFT JOIN p.category c
        WHERE p.tenantId = :tenantId
        AND p.isActive = true
        AND (
            LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))
        )
    """)
    List<Product> searchByTerm(@Param("tenantId") UUID tenantId, @Param("query") String query);
}
