package com.robotech.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// ============== EmailResetRequest.java ==============
@Data
@Schema(description = "Request para restablecer email de usuario")
public class EmailResetRequest {
    
    @NotBlank(message = "El DNI es obligatorio")
    @Size(min = 8, max = 8, message = "El DNI debe tener exactamente 8 dígitos")
    @Schema(description = "DNI del usuario", example = "12345678")
    private String dni;
    
    @NotBlank(message = "El nuevo email es obligatorio")
    @Email(message = "El formato del email no es válido")
    @Schema(description = "Nuevo email del usuario", example = "nuevoemail@gmail.com")
    private String nuevoEmail;
}