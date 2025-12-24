package com.saaspos.api.repository;

import com.saaspos.api.model.ProductPriceHistory; // Asumiendo que existe
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Repository
public interface ProductPriceHistoryRepository extends JpaRepository<ProductPriceHistory, UUID> {
    @Modifying
    @Transactional
    void deleteByProductId(UUID productId);
}