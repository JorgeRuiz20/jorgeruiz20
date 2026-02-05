package com.robotech.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_torneos")
@Getter
@Setter
public class HistorialTorneo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tipoEvento;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "torneo_id")
    private Torneo torneo;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_ganador_id")
    private User usuarioGanador;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_perdedor_id")
    private User usuarioPerdedor;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "robot_ganador_id")
    private Robot robotGanador;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "robot_perdedor_id")
    private Robot robotPerdedor;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_ganador_id")
    private Club clubGanador;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_perdedor_id")
    private Club clubPerdedor;

    private String torneoNombre;
    private String categoriaNombre;
    private String robotGanadorNombre;
    private String usuarioGanadorNombre;
    private String clubGanadorNombre;
    private String robotPerdedorNombre;
    private String usuarioPerdedorNombre;
    private String clubPerdedorNombre;
    
    private Integer puntosGanador;
    private Integer puntosPerdedor;
    private String resultado;
    
    private LocalDateTime fechaEvento;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "juez_id")
    private User juez;
    
    private String juezNombre;
    private String faseTorneo;
    
    @Column(length = 1000)
    private String detallesAdicionales;

    public HistorialTorneo() {
        this.fechaEvento = LocalDateTime.now();
    }

    public void updateNombres() {
        if (torneo != null) this.torneoNombre = torneo.getNombre();
        if (categoria != null) this.categoriaNombre = categoria.getNombre();
        if (robotGanador != null) this.robotGanadorNombre = robotGanador.getNombre();
        if (robotPerdedor != null) this.robotPerdedorNombre = robotPerdedor.getNombre();
        if (usuarioGanador != null) this.usuarioGanadorNombre = usuarioGanador.getNombre() + " " + usuarioGanador.getApellido();
        if (usuarioPerdedor != null) this.usuarioPerdedorNombre = usuarioPerdedor.getNombre() + " " + usuarioPerdedor.getApellido();
        if (clubGanador != null) this.clubGanadorNombre = clubGanador.getNombre();
        if (clubPerdedor != null) this.clubPerdedorNombre = clubPerdedor.getNombre();
        if (juez != null) this.juezNombre = juez.getNombre() + " " + juez.getApellido();
    }
}