package com.robotech.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * ✅ NUEVO: Modelo para transferencias de competidores entre clubs
 */
@Entity
@Table(name = "solicitudes_transferencia")
@Getter
@Setter
public class SolicitudTransferencia {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_solicitud", nullable = false)
    private String tipoSolicitud = "TRANSFERENCIA"; // TRANSFERENCIA o INGRESO_NUEVO

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_origen_id", nullable = true)
    private Club clubOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_destino_id", nullable = false)
    private Club clubDestino;

    /**
     * Estados posibles:
     * - PENDIENTE_SALIDA: Esperando aprobación del club actual
     * - RECHAZADA_SALIDA: Club actual rechazó
     * - PENDIENTE_INGRESO: Club actual aprobó, esperando club destino
     * - RECHAZADA_INGRESO: Club destino rechazó
     * - APROBADA: Transferencia completada
     * - CANCELADA: Usuario canceló la solicitud
     */
    @Column(nullable = false)
    private String estado = "PENDIENTE_SALIDA";

    @Column(length = 1000)
    private String mensajeUsuario;

    @Column(length = 1000)
    private String motivoRechazo;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud;

    @Column(name = "fecha_aprobacion_salida")
    private LocalDateTime fechaAprobacionSalida;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprobado_salida_por")
    private User aprobadoSalidaPor;

    @Column(name = "fecha_aprobacion_ingreso")
    private LocalDateTime fechaAprobacionIngreso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprobado_ingreso_por")
    private User aprobadoIngresoPor;

    @Column(name = "fecha_rechazo")
    private LocalDateTime fechaRechazo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rechazado_por")
    private User rechazadoPor;

    @PrePersist
    protected void onCreate() {
        this.fechaSolicitud = LocalDateTime.now();
    }

    // Métodos helper
    public boolean isPendienteSalida() {
        return "PENDIENTE_SALIDA".equals(this.estado);
    }

    public boolean isPendienteIngreso() {
        return "PENDIENTE_INGRESO".equals(this.estado);
    }

    public boolean isAprobada() {
        return "APROBADA".equals(this.estado);
    }

    public boolean isRechazada() {
        return estado != null && estado.startsWith("RECHAZADA");
    }
}