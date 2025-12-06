package com.saaspos.api.controller;

import com.saaspos.api.dto.AuthResponse;
import com.saaspos.api.dto.RegisterRequest;
import com.saaspos.api.model.User;
import com.saaspos.api.service.OnboardingService;
import com.saaspos.api.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final OnboardingService onboardingService;
    private final JwtUtil jwtUtil;

    public PublicController(OnboardingService onboardingService, JwtUtil jwtUtil) {
        this.onboardingService = onboardingService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest request) {
        try {
            // 1. Ejecutar registro protegido por token
            User newUser = onboardingService.registerWithToken(request);

            // 2. Auto-Loguear (Generar token JWT para que entre directo al dashboard)
            String token = jwtUtil.generateToken(
                    newUser.getEmail(),
                    newUser.getRole(),
                    newUser.getTenant().getId().toString()
            );

            // 3. Responder con credenciales de acceso
            return ResponseEntity.ok(new AuthResponse(
                    token,
                    newUser.getEmail(),
                    newUser.getRole(),
                    newUser.getTenant().getId().toString(),
                    newUser.getFullName()
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Opcional: Endpoint para validar si el token es válido antes de mostrar el formulario
    // GET /api/public/validate-token?token=xyz
}