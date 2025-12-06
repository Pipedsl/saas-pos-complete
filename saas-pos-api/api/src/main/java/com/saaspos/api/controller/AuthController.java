package com.saaspos.api.controller;

import com.saaspos.api.dto.AuthResponse;
import com.saaspos.api.dto.LoginRequest;
import com.saaspos.api.model.User;
import com.saaspos.api.repository.UserRepository;
import com.saaspos.api.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        // 1. Autenticar con Spring Security (esto verifica el password hash automáticamente)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // 2. Si pasa la línea de arriba, las credenciales son válidas. Buscamos al usuario completo.
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();

        // 3. Generar el Token JWT
        String tenantId = user.getTenant() != null ? user.getTenant().getId().toString() : "";
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole(), tenantId);

        // 4. Devolver respuesta
        return ResponseEntity.ok(new AuthResponse(token, user.getEmail(), user.getRole(), tenantId, user.getFullName()));
    }
}