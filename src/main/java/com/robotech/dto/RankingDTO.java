package com.robotech.dto;

import lombok.Data;

@Data
public class RankingDTO {
    private Integer posicion;
    private String nombreRobot;
    private String competidor;
    private String club;
    private Integer puntuacionTotal;
    private Integer partidosGanados;
    private Integer partidosPerdidos;
    private Integer partidosEmpatados;
    private Double efectividad;
    private Long usuarioId;
    private Long competenciaId;
    
    public RankingDTO(Integer posicion, String nombreRobot, String competidor, String club, 
                     Integer puntuacionTotal, Integer partidosGanados, Integer partidosPerdidos, 
                     Integer partidosEmpatados, Double efectividad) {
        this.posicion = posicion;
        this.nombreRobot = nombreRobot;
        this.competidor = competidor;
        this.club = club;
        this.puntuacionTotal = puntuacionTotal;
        this.partidosGanados = partidosGanados;
        this.partidosPerdidos = partidosPerdidos;
        this.partidosEmpatados = partidosEmpatados;
        this.efectividad = efectividad;
    }
}