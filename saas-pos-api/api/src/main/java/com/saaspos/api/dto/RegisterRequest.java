package com.saaspos.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "El nombre del negocio es obligatorio")
    private String companyName;

    @NotBlank(message = "Tu nombre es obligatorio")
    private String fullName;

    @Email(message = "Email inválido")
    @NotBlank(message = "El email es obligatorio")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    @NotBlank(message = "El token de invitación es obligatorio")
    private String token; // <--- ESTO ES LO NUEVO Y CRÍTICO
}