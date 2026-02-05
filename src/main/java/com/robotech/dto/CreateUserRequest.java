package com.robotech.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "Request para que el admin cree usuarios con roles")
public class CreateUserRequest {
    
    @NotBlank(message = "El nombre es obligatorio")
    @Schema(description = "Nombre del usuario", example = "Juan")
    private String nombre;
    
    @NotBlank(message = "El apellido es obligatorio")
    @Schema(description = "Apellido del usuario", example = "Perez")
    private String apellido;
    
    @NotBlank(message = "El DNI es obligatorio")
    @Size(min = 8, max = 8, message = "El DNI debe tener 8 dígitos")
    @Schema(description = "DNI del usuario", example = "12345678")
    private String dni;
    
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es válido")
    @Schema(description = "Email EXISTENTE del usuario", example = "usuario@gmail.com")
    private String email;
    
    @NotBlank(message = "El teléfono es obligatorio")
    @Schema(description = "Teléfono del usuario", example = "+51987654321")
    private String telefono;
    
    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Schema(description = "Fecha de nacimiento", example = "1990-01-01")
    private LocalDate fechaNacimiento;
    
    @NotEmpty(message = "Debe asignar al menos un rol")
    @Schema(description = "Roles del usuario", example = "[\"ROLE_CLUB_OWNER\", \"ROLE_COMPETITOR\"]")
    private List<String> roles;
    
    @Schema(description = "Foto de perfil (opcional)")
    private String fotoPerfil;
}