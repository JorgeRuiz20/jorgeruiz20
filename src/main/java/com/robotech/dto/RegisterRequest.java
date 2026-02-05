package com.robotech.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Data
@Schema(description = "Request para registro de usuario SIN robot")
public class RegisterRequest {
    
    @NotBlank(message = "El código de registro es obligatorio")
    @Schema(description = "Código de registro proporcionado por el club owner o admin", example = "REG-ABC12345")
    private String codigoRegistro;
    
    @NotBlank(message = "El DNI es obligatorio")
    @Size(min = 8, max = 8, message = "El DNI debe tener 8 dígitos")
    @Schema(description = "DNI del usuario", example = "12345678")
    private String dni;
    
    @NotBlank(message = "El nombre es obligatorio")
    @Schema(description = "Nombre del usuario", example = "Juan")
    private String nombre;
    
    @NotBlank(message = "El apellido es obligatorio")
    @Schema(description = "Apellido del usuario", example = "Perez")
    private String apellido;
    
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es válido")
    @Schema(description = "Email del usuario", example = "juan@email.com")
    private String email;
    
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$", 
             message = "La contraseña debe tener al menos una mayúscula, una minúscula, un número y un carácter especial")
    @Schema(description = "Contraseña del usuario", example = "Passw0rd!")
    private String password;
    
    @NotBlank(message = "El teléfono es obligatorio")
    @Schema(description = "Teléfono del usuario", example = "+51987654321")
    private String telefono;
    
    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Schema(description = "Fecha de nacimiento", example = "1990-01-01")
    private LocalDate fechaNacimiento;
    
    @Schema(description = "Foto de perfil en base64 o URL (opcional)")
    private String fotoPerfil;
}