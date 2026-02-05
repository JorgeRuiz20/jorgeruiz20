package com.robotech.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "enfrentamientos")
@Getter
@Setter
public class Enfrentamiento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "participante1_id")
    private Participante participante1;

    @ManyToOne
    @JoinColumn(name = "participante2_id")
    private Participante participante2;

    // ✅ RELACIÓN DIRECTA CON TORNEO (más simple)
    @ManyToOne
    @JoinColumn(name = "torneo_id")
    private Torneo torneo;


    private Integer puntosParticipante1 = 0;
    private Integer puntosParticipante2 = 0;
    private String resultado = "PENDIENTE";
    private LocalDateTime fechaEnfrentamiento;
    private String ronda;

    @ManyToOne
    @JoinColumn(name = "juez_id")
    private User juez;

    @PrePersist
    protected void onCreate() {
        if (this.fechaEnfrentamiento == null) {
            this.fechaEnfrentamiento = LocalDateTime.now();
        }
        if (this.resultado == null) {
            this.resultado = "PENDIENTE";
        }
        if (this.puntosParticipante1 == null) {
            this.puntosParticipante1 = 0;
        }
        if (this.puntosParticipante2 == null) {
            this.puntosParticipante2 = 0;
        }
    }
}