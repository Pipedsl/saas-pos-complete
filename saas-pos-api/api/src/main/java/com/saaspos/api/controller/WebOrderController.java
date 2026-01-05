package com.saaspos.api.controller;

import com.saaspos.api.model.User;
import com.saaspos.api.model.ecommerce.WebOrder;
import com.saaspos.api.repository.UserRepository;
import com.saaspos.api.repository.WebOrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/web-orders")
public class WebOrderController {

    private final WebOrderRepository webOrderRepository;
    private final UserRepository userRepository;

    public WebOrderController(WebOrderRepository webOrderRepository, UserRepository userRepository) {
        this.webOrderRepository = webOrderRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<WebOrder>> getMyWebOrders() {
        UUID tenantId = getCurrentTenantId();
        // Buscar las órdenes de MI empresa, ordenadas por fecha (más reciente primero)
        // Nota: Si no tienes un método findByTenantIdOrderByCreatedAtDesc, usa el normal y ordena en front
        List<WebOrder> orders = webOrderRepository.findByTenantId(tenantId);
        return ResponseEntity.ok(orders);
    }

    private UUID getCurrentTenantId(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return user.getTenant().getId();
    }
}