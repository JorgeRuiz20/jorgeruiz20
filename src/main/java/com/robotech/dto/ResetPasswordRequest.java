package com.robotech.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request para restablecer contraseña")
public class ResetPasswordRequest {
    
    @NotBlank(message = "El token es obligatorio")
    @Schema(description = "Token de recuperación recibido por email")
    private String token;
    
    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$", 
             message = "La contraseña debe tener al menos una mayúscula, una minúscula, un número y un carácter especial")
    @Schema(description = "Nueva contraseña", example = "NewPassword123!")
    private String newPassword;
    
    @NotBlank(message = "La confirmación de contraseña es obligatoria")
    @Schema(description = "Confirmación de la nueva contraseña")
    private String confirmPassword;
}