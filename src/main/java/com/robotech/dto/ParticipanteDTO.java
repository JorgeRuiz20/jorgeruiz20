package com.robotech.dto;

import lombok.Data;

@Data
public class ParticipanteDTO {
    private Long id;
    private String nombreRobot;
    private String descripcionRobot;
    private Integer puntuacionTotal;
    private Integer partidosGanados;
    private Integer partidosPerdidos;
    private Integer partidosEmpatados;
    private Double efectividad;
    private Long usuarioId;
    private String usuarioNombre;
    private String usuarioEmail;
    private String usuarioDni;
    private Long clubId;
    private String clubNombre;
    private Long competenciaId;
    private String competenciaNombre;
    private Long categoriaId;
    private String categoriaNombre;
}