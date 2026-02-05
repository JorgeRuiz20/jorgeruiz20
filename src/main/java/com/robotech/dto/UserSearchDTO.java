package com.robotech.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
class UserSearchDTO {
    private Long id;
    private String nombre;
    private String apellido;
    private String dni;
    private String email;
    private String estado;
    private String clubNombre;
    private java.util.List<String> roles;
}