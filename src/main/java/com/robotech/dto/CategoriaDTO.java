package com.robotech.dto;

import lombok.Data;

@Data
public class CategoriaDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    private Integer edadMinima;
    private Integer edadMaxima;
    private Integer pesoMaximo;
    private String reglasEspecificas;
    private Boolean activa;
    private Integer robotsCount;
    private Integer competenciasCount;

    public boolean isActiva() {
        return Boolean.TRUE.equals(activa);
    }
}