package com.saaspos.api.controller;

import com.saaspos.api.dto.CreateUserDto;
import com.saaspos.api.dto.UserResponseDto;
import com.saaspos.api.model.User;
import com.saaspos.api.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // --- LISTAR EMPLEADOS (CORREGIDO) ---
    // Ahora devuelve List<UserResponseDto> en lugar de List<User>
    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getMyUsers() {
        User currentUser = getCurrentUser();

        // Si el usuario no tiene tenant (ej: Super Admin global), devolvemos lista vacía o manejamos error
        if (currentUser.getTenant() == null) {
            return ResponseEntity.ok(List.of());
        }

        List<User> users = userRepository.findByTenantId(currentUser.getTenant().getId());

        // Convertir Entidades a DTOs para evitar el error de "ByteBuddyInterceptor"
        List<UserResponseDto> dtos = users.stream().map(this::mapToDto).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // --- CREAR EMPLEADO ---
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody CreateUserDto dto) {
        User currentUser = getCurrentUser();

        // 1. Logs de depuración (útil para ver qué pasa)
        System.out.println("Creando usuario con rol: " + dto.getRole());
        System.out.println("Solicitante: " + currentUser.getRole());

        // 2. Validación General (Solo Admins pueden crear gente)
        if (!currentUser.getRole().contains("ADMIN")) {
            return ResponseEntity.status(403).body("No tienes permisos para crear usuarios.");
        }

        // --- NUEVA VALIDACIÓN DE SEGURIDAD (VENDOR) ---
        // Si intentan crear un VENDOR, verificamos que quien lo pide sea SUPER_ADMIN
        if ("VENDOR".equals(dto.getRole()) && !"SUPER_ADMIN".equals(currentUser.getRole())) {
            return ResponseEntity.status(403).body("Solo el Super Admin puede crear Vendedores SaaS (Partners).");
        }
        // ----------------------------------------------

        if (userRepository.existsByEmail(dto.getEmail())) {
            return ResponseEntity.badRequest().body("El email ya está registrado.");
        }

        // 3. Crear Entidad
        User newUser = new User();
        newUser.setFullName(dto.getFullName());
        newUser.setEmail(dto.getEmail());
        newUser.setPassword(passwordEncoder.encode(dto.getPassword()));

        // Asignar rol (o default)
        String roleToAssign = dto.getRole();
        if (roleToAssign == null || roleToAssign.trim().isEmpty()) {
            roleToAssign = "CASHIER";
        }
        newUser.setRole(roleToAssign);

        newUser.setTenant(currentUser.getTenant());
        newUser.setActive(true);

        User savedUser = userRepository.save(newUser);

        return ResponseEntity.ok(mapToDto(savedUser));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getMe() {
        User user = getCurrentUser(); // Ya tienes este método privado abajo
        return ResponseEntity.ok(mapToDto(user)); // Reusamos el mapper
    }

    // Helper para convertir User -> DTO
    private UserResponseDto mapToDto(User user) {
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setActive(user.isActive());
        return dto;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName()).orElseThrow();
    }
}