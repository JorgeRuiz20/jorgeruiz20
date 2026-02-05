package com.robotech.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TorneoDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    
    // ✅ ACTUALIZADO: Información de la sede
    private Long sedeId;
    private String sedeNombre;
    private String sedeDireccion;
    private String sedeDistrito;
    
    private Long categoriaId;
    private String categoriaNombre;
    private String categoriaDescripcion;
    private Integer edadMinima;
    private Integer edadMaxima;
    private Integer pesoMaximo;
    private String estado;
    
    // Campos existentes
    private String modalidad;
    private String faseActual;
    private Long juezResponsableId;
    private String juezResponsableNombre;
    
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    
    // ✅ NUEVO: Programación automática
    private LocalDateTime fechaActivacionProgramada;
    private Boolean activacionAutomatica;
    
    // ✅ NUEVO: Tiempo restante en segundos (para el contador)
    private Long segundosRestantes;
    
    private Integer cantidadCompetencias;
    private Integer cantidadParticipantes;
}