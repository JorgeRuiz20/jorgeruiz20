package com.robotech.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Set;

@Data
@Builder
public class AuthResponse {
    private String token;
    private String email;
    private String nombre;
    private Set<String> roles;
    private String estado;
    private Long clubId;
    private String dni;
    private String telefono;
}