package com.robotech.models.views;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;

@Entity
@Immutable
@Subselect("SELECT * FROM vista_participantes_detalle")
@Getter
@Setter
public class VistaParticipanteDetalle {

    @Id
    @Column(name = "participante_id")
    private Long participanteId;

    @Column(name = "nombre_robot")
    private String nombreRobot;

    @Column(name = "descripcion_robot")
    private String descripcionRobot;

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

    @Column(name = "torneo_estado")
    private String torneoEstado;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "usuario_nombre")
    private String usuarioNombre;

    @Column(name = "usuario_dni")
    private String usuarioDni;

    @Column(name = "usuario_email")
    private String usuarioEmail;

    @Column(name = "usuario_telefono")
    private String usuarioTelefono;

    @Column(name = "club_id")
    private Long clubId;

    @Column(name = "club_nombre")
    private String clubNombre;

    @Column(name = "club_ciudad")
    private String clubCiudad;

    @Column(name = "club_pais")
    private String clubPais;

    @Column(name = "robot_id")
    private Long robotId;

    @Column(name = "robot_peso")
    private Integer robotPeso;

    @Column(name = "robot_codigo")
    private String robotCodigo;

    @Column(name = "categoria_nombre")
    private String categoriaNombre;
}
