package com.robotech.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "categorias")
@Getter
@Setter
public class Categoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String descripcion;
    private Integer edadMinima;
    private Integer edadMaxima;
    private Integer pesoMaximo;
    
    @Column(length = 1000)
    private String reglasEspecificas;
    private Boolean activa = true;
}