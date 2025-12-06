package com.saaspos.api.controller;

import com.saaspos.api.dto.SaleRequest;
import com.saaspos.api.model.Sale;
import com.saaspos.api.model.User;
import com.saaspos.api.repository.UserRepository;
import com.saaspos.api.service.SaleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private final SaleService saleService;
    private final UserRepository userRepository;

    public SaleController(SaleService saleService, UserRepository userRepository) {
        this.saleService = saleService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<?> createSale(@RequestBody SaleRequest request) {
        try {
            UUID tenantId = getCurrentTenantId();
            Sale sale = saleService.processSale(request, tenantId);
            return ResponseEntity.ok(sale);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private UUID getCurrentTenantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        return user.getTenant().getId();
    }
}