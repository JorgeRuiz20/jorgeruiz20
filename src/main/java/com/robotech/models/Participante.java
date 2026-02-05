package com.robotech.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "participantes")
@Getter
@Setter
public class Participante {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private User usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "torneo_id")
    private Torneo torneo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "robot_id", nullable = false)
    private Robot robot;

    private String nombreRobot;
    private String descripcionRobot;
    private String fotoRobot;
    private Integer puntuacionTotal = 0;
    private Integer partidosGanados = 0;
    private Integer partidosPerdidos = 0;
    private Integer partidosEmpatados = 0;
}