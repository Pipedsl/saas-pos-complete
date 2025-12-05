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

        // 1. Validar Rol
        if (!agent.getRole().contains("ADMIN") && !agent.getRole().equals("VENDOR")) {
            throw new RuntimeException("No tienes permisos para generar links.");
        }

        // 2. VALIDAR LÍMITE MENSUAL (DESCOMENTADO Y MEJORADO)
        // Calculamos el primer día del mes actual (ej: 1 de Diciembre 00:00)
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        // Contamos cuántos links ha hecho este agente este mes
        long linksCreated = demoLinkRepository.countByAgentIdAndCreatedAtAfter(agent.getId(), startOfMonth);

        // Verificamos el límite (si es null, asumimos infinito o ponemos un default como 10)
        int limit = agent.getLinkLimitMonthly() != null ? agent.getLinkLimitMonthly() : 10;

        if (linksCreated >= limit) {
            throw new RuntimeException("Has alcanzado tu límite de " + limit + " demos este mes. Contacta al Super Admin.");
        }

        // 3. Crear el Link
        DemoLink link = new DemoLink();
        link.setAgent(agent);
        // Generamos un token limpio (sin guiones)
        link.setToken(UUID.randomUUID().toString().replace("-", ""));
        // El link es válido por 30 días para ser activado
        link.setExpiresAt(LocalDateTime.now().plusDays(30));
        link.setUsed(false);

        return demoLinkRepository.save(link);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName()).orElseThrow();
    }
}