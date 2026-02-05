package com.robotech.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SolicitudTransferenciaDTO {
    private Long id;
    
    // Usuario
    private Long usuarioId;
    private String usuarioNombre;
    private String usuarioEmail;
    private String usuarioDni;
    private String usuarioFoto;
    
    // Club Origen
    private Long clubOrigenId;
    private String clubOrigenNombre;
    private String clubOrigenCiudad;
    
    // Club Destino
    private Long clubDestinoId;
    private String clubDestinoNombre;
    private String clubDestinoCiudad;
    
    // Estado
    private String estado;
    private String mensajeUsuario;
    private String motivoRechazo;
    
    // Fechas
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaAprobacionSalida;
    private LocalDateTime fechaAprobacionIngreso;
    private LocalDateTime fechaRechazo;
    
    // Aprobadores
    private String aprobadoSalidaPorNombre;
    private String aprobadoIngresoPorNombre;
    private String rechazadoPorNombre;
    
    // Info adicional
    private Integer robotsDelUsuario;
    private Integer cuposDisponiblesDestino;
}