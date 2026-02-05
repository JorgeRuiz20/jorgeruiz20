package com.robotech.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clubs")
@Getter
@Setter
public class Club {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String descripcion;
    private String logo;
    private String ciudad;
    private String pais;
    

    @Column(name = "activa")
    private Boolean activa = true;

    @Column(name = "max_participantes")
    private Integer maxParticipantes = 16; // Máximo 16 participantes por club

    @OneToMany(mappedBy = "club", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<User> miembros = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;


        // ✅ NUEVO: Método helper
    public boolean isActiva() {
        return Boolean.TRUE.equals(this.activa);
    }

    // Método para verificar si el club está lleno
    public boolean isFull() {
        return miembros != null && miembros.size() >= maxParticipantes;
    }

    // Método para obtener cupos disponibles
    public int getCuposDisponibles() {
        int ocupados = (miembros != null) ? miembros.size() : 0;
        return maxParticipantes - ocupados;
    }
}