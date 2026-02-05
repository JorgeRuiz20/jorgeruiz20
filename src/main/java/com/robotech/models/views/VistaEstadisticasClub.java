package com.robotech.models.views;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;

@Entity
@Immutable
@Subselect("SELECT * FROM vista_estadisticas_club")
@Getter
@Setter
public class VistaEstadisticasClub {

    @Id
    @Column(name = "club_id")
    private Long clubId;

    @Column(name = "club_nombre")
    private String clubNombre;

    private String ciudad;
    private String pais;

    @Column(name = "club_owner")
    private String clubOwner;

    @Column(name = "total_miembros")
    private Long totalMiembros;

    @Column(name = "total_robots")
    private Long totalRobots;

    @Column(name = "total_participaciones")
    private Long totalParticipaciones;

    @Column(name = "total_victorias")
    private Long totalVictorias;

    @Column(name = "total_derrotas")
    private Long totalDerrotas;

    @Column(name = "total_empates")
    private Long totalEmpates;

    @Column(name = "puntuacion_acumulada")
    private Long puntuacionAcumulada;

    @Column(name = "torneos_ganados")
    private Long torneosGanados;
}
