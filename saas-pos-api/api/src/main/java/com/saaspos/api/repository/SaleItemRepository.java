package com.saaspos.api.repository;

import com.saaspos.api.model.SaleItem;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface SaleItemRepository extends JpaRepository<SaleItem, UUID> {
    // Método crítico para el Hard Delete
    @Modifying
    @Transactional
    @Query("DELETE FROM SaleItem s WHERE s.product.id = :productId")
    void deleteByProductId(UUID productId);
}