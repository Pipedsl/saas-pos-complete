package com.saaspos.api.controller;

import com.saaspos.api.model.DemoLink;
import com.saaspos.api.service.SalesAgentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class SalesAgentController {

    private final SalesAgentService salesAgentService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public SalesAgentController(SalesAgentService salesAgentService) {
        this.salesAgentService = salesAgentService;
    }

    @PostMapping("/generate-link")
    public ResponseEntity<?> createLink() {
        try {
            DemoLink link = salesAgentService.generateDemoLink();

            // Devolvemos la URL completa para que el vendedor la copie
            // (Ajusta 'localhost:4200' por tu dominio real en producción)
            String fullUrl = frontendUrl + "/register?token=" + link.getToken();

            return ResponseEntity.ok(Map.of(
                    "url", fullUrl,
                    "token", link.getToken(),
                    "expiresAt", link.getExpiresAt()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}