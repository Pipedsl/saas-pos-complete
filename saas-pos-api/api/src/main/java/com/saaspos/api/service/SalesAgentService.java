package com.saaspos.api.service;

import com.saaspos.api.model.DemoLink;
import com.saaspos.api.model.User;
import com.saaspos.api.repository.DemoLinkRepository;
import com.saaspos.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SalesAgentService {

    private final DemoLinkRepository demoLinkRepository;
    private final UserRepository userRepository;

    public SalesAgentService(DemoLinkRepository demoLinkRepository, UserRepository userRepository) {
        this.demoLinkRepository = demoLinkRepository;
        this.userRepository = userRepository;
    }

    public DemoLink generateDemoLink() {
        User agent = getCurrentUser();

        // 1. Validar Permisos (Roles)
        if (!agent.getRole().contains("ADMIN") && !agent.getRole().equals("VENDOR")) {
            throw new RuntimeException("No tienes permisos para generar links.");
        }

        // 2. VALIDAR LÍMITE (SOLO SI NO ES SUPER ADMIN)
        // Agregamos esta condición: Si es SUPER_ADMIN, se salta este bloque
        if (!"SUPER_ADMIN".equals(agent.getRole())) {

            LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            long linksCreated = demoLinkRepository.countByAgentIdAndCreatedAtAfter(agent.getId(), startOfMonth);

            // Límite por defecto 10 si no está definido
            int limit = agent.getLinkLimitMonthly() != null ? agent.getLinkLimitMonthly() : 10;

            if (linksCreated >= limit) {
                throw new RuntimeException("Has alcanzado tu límite de " + limit + " demos este mes.");
            }
        }

        // 3. Crear el Link
        DemoLink link = new DemoLink();
        link.setAgent(agent);
        link.setToken(UUID.randomUUID().toString().replace("-", ""));
        link.setExpiresAt(LocalDateTime.now().plusDays(30));
        link.setUsed(false);

        return demoLinkRepository.save(link);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName()).orElseThrow();
    }
}