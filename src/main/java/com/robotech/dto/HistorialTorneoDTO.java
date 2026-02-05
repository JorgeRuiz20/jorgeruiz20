package com.robotech.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class HistorialTorneoDTO {
    private Long id;
    private String tipoEvento;
    private Long entidadId;
    private String entidadTipo;
    private String torneoNombre;
    private String categoriaNombre;
    private String robotGanador;
    private String usuarioGanador;
    private String clubGanador;
    private String robotPerdedor;
    private String usuarioPerdedor;
    private String clubPerdedor;
    private Integer puntosGanador;
    private Integer puntosPerdedor;
    private String resultado;
    private LocalDateTime fechaEvento;
    private String juezNombre;
    private String faseTorneo;
    private String detallesAdicionales;
    private Long usuarioGanadorId;
    private Long usuarioPerdedorId;
}