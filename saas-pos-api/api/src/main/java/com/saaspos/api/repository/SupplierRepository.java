package com.saaspos.api.repository;
import com.saaspos.api.model.Supplier; // Asegúrate de tener la Entity Supplier (o créala rápido si falta)
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {
    List<Supplier> findByTenantId(UUID tenantId);
}