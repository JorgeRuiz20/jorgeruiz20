package com.robotech.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "torneos")
@Getter
@Setter
public class Torneo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;
    
    private String descripcion;

    // ✅ ACTUALIZADO: Ahora es relación con Sede
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id")
    private Sede sede;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(nullable = false)
    private String estado = "PENDIENTE"; // PENDIENTE, ACTIVO, FINALIZADO

    // ✅ NUEVO: Programación automática
    @Column(name = "fecha_activacion_programada")
    private LocalDateTime fechaActivacionProgramada;

    @Column(name = "activacion_automatica")
    private Boolean activacionAutomatica = false;

    @Column(name = "modalidad")
    private String modalidad; // ELIMINATORIA, TODOS_CONTRA_TODOS

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "juez_responsable_id")
    private User juezResponsable;

    @Column(name = "fase_actual")
    private String faseActual; // OCTAVOS, CUARTOS, SEMIFINAL, FINAL

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    // Relaciones directas
    @OneToMany(mappedBy = "torneo", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Participante> participantes = new ArrayList<>();

    @OneToMany(mappedBy = "torneo", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Enfrentamiento> enfrentamientos = new ArrayList<>();

  
    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }

    /**
     * ✅ Método para verificar si el torneo debe activarse automáticamente
     */
    public boolean debeActivarseAutomaticamente() {
        return Boolean.TRUE.equals(activacionAutomatica) 
            && fechaActivacionProgramada != null 
            && LocalDateTime.now().isAfter(fechaActivacionProgramada)
            && "PENDIENTE".equals(estado);
    }
}