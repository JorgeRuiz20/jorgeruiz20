package com.robotech.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.Set;

@Data
public class UserDTO {
    private Long id;
    private String nombre;
    private String apellido;
    private String dni;
    private String email;
    private String fotoPerfil;
    private String estado;
    private String telefono;
    private LocalDate fechaNacimiento;
    private Set<String> roles;
    private Long clubId;
    private String clubNombre;
}