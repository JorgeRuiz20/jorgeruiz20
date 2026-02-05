package com.robotech.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request para login de usuario")
public class LoginRequest {
    @Schema(description = "Email del usuario", example = "admin@robotech.com")
    private String email;
    
    @Schema(description = "Contraseña del usuario", example = "admin123")
    private String password;
}