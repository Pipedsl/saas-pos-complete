package com.saaspos.api.repository;

import com.saaspos.api.model.DemoLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface DemoLinkRepository extends JpaRepository<DemoLink, UUID> {
    // Buscar por token (para validar cuando el cliente entra)
    Optional<DemoLink> findByToken(String token);

    // Contar cuántos links ha hecho un agente este mes (para el límite)
    // (Nota: Esta query es simple, luego podemos hacerla más precisa por fechas)
    long countByAgentId(UUID agentId);

    // Contar links de un agente creados DESPUÉS de una fecha específica
    long countByAgentIdAndCreatedAtAfter(UUID agentId, LocalDateTime date);
}