package com.robotech.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "codigos_registro")
@Getter
@Setter
public class CodigoRegistro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String codigo;

    private Boolean usado = false;
    
    private LocalDateTime fechaGeneracion;
    private LocalDateTime fechaUso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generado_por_id")
    private User generadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usado_por_id")
    private User usadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id")
    private Club club;

    public CodigoRegistro() {
        this.fechaGeneracion = LocalDateTime.now();
    }

    public CodigoRegistro(String codigo, User generadoPor, Club club) {
        this.codigo = codigo;
        this.generadoPor = generadoPor;
        this.club = club;
        this.fechaGeneracion = LocalDateTime.now();
        this.usado = false;
    }
}