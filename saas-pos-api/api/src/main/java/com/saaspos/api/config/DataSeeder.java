package com.saaspos.api.config;

import com.saaspos.api.model.Plan;
import com.saaspos.api.model.Tenant;
import com.saaspos.api.model.User;
import com.saaspos.api.repository.PlanRepository;
import com.saaspos.api.repository.TenantRepository;
import com.saaspos.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataSeeder implements CommandLineRunner{

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PlanRepository planRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.default-password}")
    private String adminPassword;

    //Inyeccion de dependencias por constructor (Best Practice)
    public DataSeeder(UserRepository userRepository, TenantRepository tenantRepository, PlanRepository planRepository,PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.planRepository = planRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        String email = "felipe@webiados.com";

        // Si ya existe el admin, no hacemos nada
        if (userRepository.existsByEmail(email)) {
            return;
        }

        // 1. Buscar o Crear el Plan DEV
        Plan devPlan = planRepository.findByName("PLAN_DEV");
        if (devPlan == null) {
            devPlan = new Plan();
            devPlan.setName("PLAN_DEV");
            devPlan.setPriceClp(BigDecimal.ZERO);
            devPlan.setMaxUsers(100);
            devPlan.setMaxProducts(1000);
            planRepository.save(devPlan);
        }

        // 2. Buscar o Crear el Tenant "Webiados"
        String rutDev = "99.999.999-k";
        Tenant devTenant = tenantRepository.findByRut(rutDev).orElse(null);

        if (devTenant == null) {
            devTenant = new Tenant();
            devTenant.setCompanyName("Webiados");
            devTenant.setRut(rutDev);
            devTenant.setPlan(devPlan);
            devTenant.setActive(true);
            tenantRepository.save(devTenant);
            System.out.println("--- SEEDER: Tenant 'Webiados' creado automáticamente ---");
        }

        // 3. Crear Usuario Admin
        User admin = new User();
        admin.setEmail(email);
        admin.setFullName("Super Admin");
        admin.setRole("SUPER_ADMIN");
        admin.setTenant(devTenant);
        admin.setPassword(passwordEncoder.encode(adminPassword));

        userRepository.save(admin);
        System.out.println("--- SEEDER: Usuario Admin creado ---");
    }
}
