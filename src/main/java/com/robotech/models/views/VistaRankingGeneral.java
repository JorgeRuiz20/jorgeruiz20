package com.robotech.models.views;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;

@Entity
@Immutable
@Subselect("SELECT * FROM vista_ranking_general")
@Getter
@Setter
public class VistaRankingGeneral {

    @Id
    @Column(name = "participante_id")
    private Long participanteId;

    private Integer posicion;

    @Column(name = "nombre_robot")
    private String nombreRobot;

    @Column(name = "puntuacion_total")
    private Integer puntuacionTotal;

    @Column(name = "partidos_ganados")
    private Integer partidosGanados;

    @Column(name = "partidos_perdidos")
    private Integer partidosPerdidos;

    @Column(name = "partidos_empatados")
    private Integer partidosEmpatados;

    private Double efectividad;

    @Column(name = "torneo_id")
    private Long torneoId;

    @Column(name = "torneo_nombre")
    private String torneoNombre;

    private String competidor;

    @Column(name = "club_nombre")
    private String clubNombre;

    @Column(name = "categoria_nombre")
    private String categoriaNombre;
}
