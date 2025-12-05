package com.saaspos.api.repository;

import com.saaspos.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    //Spring Data JPA crea la querty automaticamente basada en el nombre
    Optional<User> findByEmail(String email);

    //Para validar que no se repita al registrarse
    boolean existsByEmail(String email);

    List<User> findByTenantId(UUID tenantId);
}