package com.saaspos.api.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class UserResponseDto {
    private UUID id;
    private String fullName;
    private String email;
    private String role;
    private boolean isActive;
}
