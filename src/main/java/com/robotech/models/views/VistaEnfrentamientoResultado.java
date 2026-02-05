package com.robotech.models.views;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;

import java.time.LocalDateTime;

@Entity
@Immutable
@Subselect("SELECT * FROM vista_enfrentamientos_resultado")
@Getter
@Setter
public class VistaEnfrentamientoResultado {

    @Id
    @Column(name = "enfrentamiento_id")
    private Long enfrentamientoId;

    @Column(name = "fecha_enfrentamiento")
    private LocalDateTime fechaEnfrentamiento;

    private String ronda;
    private String resultado;

    @Column(name = "puntos_participante1")
    private Integer puntosParticipante1;

    @Column(name = "puntos_participante2")
    private Integer puntosParticipante2;

    @Column(name = "torneo_id")
    private Long torneoId;

    @Column(name = "torneo_nombre")
    private String torneoNombre;

    private String modalidad;

    @Column(name = "participante1_id")
    private Long participante1Id;

    @Column(name = "participante1_robot")
    private String participante1Robot;

    @Column(name = "participante1_usuario")
    private String participante1Usuario;

    @Column(name = "participante1_club")
    private String participante1Club;

    @Column(name = "participante2_id")
    private Long participante2Id;

    @Column(name = "participante2_robot")
    private String participante2Robot;

    @Column(name = "participante2_usuario")
    private String participante2Usuario;

    @Column(name = "participante2_club")
    private String participante2Club;

    @Column(name = "juez_nombre")
    private String juezNombre;

    @Column(name = "fase_nombre")
    private String faseNombre;

    @Column(name = "categoria_nombre")
    private String categoriaNombre;

    @Column(name = "ganador_robot")
    private String ganadorRobot;
}
