package com.saaspos.api.repository;

import com.saaspos.api.model.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SaleItemRepository extends JpaRepository<SaleItem, UUID> {
}