package com.robotech.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "robots")
@Getter
@Setter
public class Robot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    
    @Column(length = 1000)
    private String descripcion;
    
    private String fotoRobot;
    private Integer peso;
    
    @Column(length = 1000)
    private String especificacionesTecnicas;
    
    @Column(unique = true)
    private String codigoIdentificacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private User usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    private String estado = "PENDIENTE";
    
    // ✅ CAMPO PARA MOTIVO DE RECHAZO
    @Column(length = 1000)
    private String motivoRechazo;
}