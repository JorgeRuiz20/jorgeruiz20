package com.robotech.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * Modelo para sedes de torneos
 * Almacena ubicaciones predefinidas en Lima, Perú
 */
@Entity
@Table(name = "sedes")
@Getter
@Setter
public class Sede {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    private String direccion;

    private String distrito;

    private String referencia;

    @Column(name = "capacidad_maxima")
    private Integer capacidadMaxima;

    @Column(name = "tiene_estacionamiento")
    private Boolean tieneEstacionamiento = false;

    private Boolean activa = true;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }
}