package com.robotech.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * ✅ NUEVO: Modelo para gestionar la deshabilitación de clubs
 */
@Entity
@Table(name = "club_deshabilitaciones")
@Getter
@Setter
public class ClubDeshabilitacion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @Column(nullable = false)
    private String motivo;

    /**
     * Estados:
     * - PENDIENTE: Notificación enviada, esperando acciones de usuarios
     * - PROCESANDO: Admin enviando solicitudes masivas
     * - COMPLETADA: Todos reubicados o degradados
     * - CANCELADA: Admin canceló la deshabilitación
     */
    @Column(nullable = false)
    private String estado = "PENDIENTE";

    @Column(name = "fecha_deshabilitacion", nullable = false)
    private LocalDateTime fechaDeshabilitacion;

    @Column(name = "fecha_limite_accion", nullable = false)
    private LocalDateTime fechaLimiteAccion;

    @Column(name = "fecha_completada")
    private LocalDateTime fechaCompletada;

    @Column(name = "total_miembros", nullable = false)
    private Integer totalMiembros;

    @Column(name = "miembros_reubicados")
    private Integer miembrosReubicados = 0;

    @Column(name = "miembros_degradados")
    private Integer miembrosDegradados = 0;

    @Column(name = "notificaciones_enviadas")
    private Boolean notificacionesEnviadas = false;

    @Column(length = 2000)
    private String observaciones;

    @PrePersist
    protected void onCreate() {
        this.fechaDeshabilitacion = LocalDateTime.now();
        // 7 días para que usuarios actúen
        this.fechaLimiteAccion = LocalDateTime.now().plusDays(7);
    }

    public boolean isPendiente() {
        return "PENDIENTE".equals(this.estado);
    }

    public boolean isProcesando() {
        return "PROCESANDO".equals(this.estado);
    }

    public boolean isCompletada() {
        return "COMPLETADA".equals(this.estado);
    }

    public boolean limiteExpirado() {
        return LocalDateTime.now().isAfter(this.fechaLimiteAccion);
    }
}