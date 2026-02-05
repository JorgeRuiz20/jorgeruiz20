package com.robotech.dto;

import lombok.Data;

@Data
public class RobotDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    private String fotoRobot;
    private Integer peso;
    private String especificacionesTecnicas;
    private String codigoIdentificacion;
    private Long usuarioId;
    private String usuarioNombre;
    private Long categoriaId;
    private String categoriaNombre;
    private String estado;
    private Long clubId;
    private String clubNombre;
    
    // ✅ NUEVO: Motivo de rechazo
    private String motivoRechazo;
}