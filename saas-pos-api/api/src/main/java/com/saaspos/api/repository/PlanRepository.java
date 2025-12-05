package com.saaspos.api.repository;

import com.saaspos.api.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PlanRepository extends JpaRepository<Plan, UUID> {
    // Método mágico para buscar por nombre (usado en el onboarding para buscar el plan "DEMO")
    Plan findByName(String name);
}