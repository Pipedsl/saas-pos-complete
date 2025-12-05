package com.saaspos.api.service;

import com.saaspos.api.dto.RegisterRequest;
import com.saaspos.api.model.DemoLink;
import com.saaspos.api.model.Plan;
import com.saaspos.api.model.Tenant;
import com.saaspos.api.model.User;
import com.saaspos.api.repository.DemoLinkRepository;
import com.saaspos.api.repository.PlanRepository;
import com.saaspos.api.repository.TenantRepository;
import com.saaspos.api.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class OnboardingService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final DemoLinkRepository demoLinkRepository;
    private final PasswordEncoder passwordEncoder;

    public OnboardingService(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            PlanRepository planRepository,
            DemoLinkRepository demoLinkRepository,
            PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.planRepository = planRepository;
        this.demoLinkRepository = demoLinkRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerWithToken(RegisterRequest request) {
        // 1. VALIDAR EL TOKEN
        DemoLink link = demoLinkRepository.findByToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Token de invitación inválido"));

        if (link.isUsed()) {
            throw new RuntimeException("Este link de invitación ya fue utilizado.");
        }

        if (link.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Este link de invitación ha expirado.");
        }

        // 2. Validar Email Duplicado
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El email ya está registrado en el sistema.");
        }

        // 3. Obtener Plan DEMO (Crea uno por defecto si no existe)
        Plan demoPlan = planRepository.findByName("DEMO");
        if (demoPlan == null) {
            demoPlan = new Plan();
            demoPlan.setName("DEMO");
            demoPlan.setMaxUsers(1);
            demoPlan.setMaxProducts(50);
            demoPlan.setPriceClp(BigDecimal.ZERO);
            planRepository.save(demoPlan);
        }

        // 4. Crear el Tenant (La Empresa)
        Tenant tenant = new Tenant();
        tenant.setCompanyName(request.getCompanyName());
        tenant.setRut("Por definir");
        tenant.setPlan(demoPlan);
        tenant.setActive(true);
        // Configuramos la expiración de la cuenta (7 días desde hoy)
        tenant.setDemoExpiresAt(LocalDateTime.now().plusDays(7));
        tenant.setIsDemoAccount(true);

        tenantRepository.save(tenant);

        // 5. Crear el Dueño (User)
        User admin = new User();
        admin.setFullName(request.getFullName());
        admin.setEmail(request.getEmail());
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        admin.setRole("TENANT_ADMIN");
        admin.setTenant(tenant);
        admin.setActive(true);

        User savedUser = userRepository.save(admin);

        // 6. QUEMAR EL TOKEN (Marcar como usado)
        link.setUsed(true);
        link.setUsedAt(LocalDateTime.now());
        link.setCreatedTenantId(tenant.getId()); // Guardamos referencia de quién lo usó
        demoLinkRepository.save(link);

        return savedUser;
    }
}