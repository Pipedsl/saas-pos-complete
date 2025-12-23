package com.saaspos.api.repository;

import com.saaspos.api.model.ecommerce.WebOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WebOrderRepository extends JpaRepository<WebOrder, UUID> {
}
