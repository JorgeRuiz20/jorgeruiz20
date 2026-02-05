package com.robotech.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ClubDeshabilitacionDTO {
    private Long id;
    private Long clubId;
    private String clubNombre;
    private String adminNombre;
    private String motivo;
    private String estado;
    private LocalDateTime fechaDeshabilitacion;
    private LocalDateTime fechaLimiteAccion;
    private LocalDateTime fechaCompletada;
    private Integer totalMiembros;
    private Integer miembrosReubicados;
    private Integer miembrosDegradados;
    private Integer miembrosPendientes;
    private Boolean notificacionesEnviadas;
    private String observaciones;
    private Long diasRestantes;
    private Boolean limiteExpirado;
    
    // Lista de miembros afectados
    private List<MiembroAfectadoDTO> miembrosAfectados;
}

@Data
class MiembroAfectadoDTO {
    private Long usuarioId;
    private String nombre;
    private String apellido;
    private String email;
    private String dni;
    private Integer cantidadRobots;
    private String estadoReubicacion; // PENDIENTE, TRANSFERIDO, DEGRADADO
    private Long clubDestinoId;
    private String clubDestinoNombre;
}