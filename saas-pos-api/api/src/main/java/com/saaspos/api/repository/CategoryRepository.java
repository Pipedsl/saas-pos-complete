package com.saaspos.api.repository;

import com.saaspos.api.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    //Buscar todas las categorias activas de un tenant
    List<Category> findByTenantIdAndIsActiveTrue(UUID tenantId);
}
