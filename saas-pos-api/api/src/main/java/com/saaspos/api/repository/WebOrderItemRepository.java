package com.saaspos.api.repository;

import com.saaspos.api.model.ecommerce.WebOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Repository
public interface WebOrderItemRepository extends JpaRepository<WebOrderItem, UUID> {
    @Modifying
    @Transactional
    void deleteByProductId(UUID productId);
}