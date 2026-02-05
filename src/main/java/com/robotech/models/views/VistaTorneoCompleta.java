package com.robotech.models.views;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;

import java.time.LocalDateTime;

@Entity
@Immutable
@Subselect("SELECT * FROM vista_torneos_completa")
@Getter
@Setter
public class VistaTorneoCompleta {

    @Id
    @Column(name = "torneo_id")
    private Long torneoId;

    @Column(name = "torneo_nombre")
    private String torneoNombre;

    @Column(name = "torneo_descripcion")
    private String torneoDescripcion;

    @Column(name = "torneo_estado")
    private String torneoEstado;

    private String modalidad;

    @Column(name = "fase_actual")
    private String faseActual;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "categoria_id")
    private Long categoriaId;

    @Column(name = "categoria_nombre")
    private String categoriaNombre;

    @Column(name = "edad_minima")
    private Integer edadMinima;

    @Column(name = "edad_maxima")
    private Integer edadMaxima;

    @Column(name = "peso_maximo")
    private Integer pesoMaximo;

    @Column(name = "juez_id")
    private Long juezId;

    @Column(name = "juez_nombre")
    private String juezNombre;

    @Column(name = "total_participantes")
    private Long totalParticipantes;

    @Column(name = "total_enfrentamientos")
    private Long totalEnfrentamientos;

    @Column(name = "enfrentamientos_pendientes")
    private Long enfrentamientosPendientes;

    @Column(name = "enfrentamientos_completados")
    private Long enfrentamientosCompletados;
}
