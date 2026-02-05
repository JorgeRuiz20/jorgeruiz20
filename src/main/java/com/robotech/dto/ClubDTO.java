package com.robotech.dto;

import lombok.Data;

@Data
public class ClubDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    private String ciudad;
    private String pais;
    private String logo;
    private Long ownerId;
    private String ownerNombre;
    private Integer cantidadMiembros;

        // ✅ AGREGAR ESTOS CAMPOS
    private Integer maxParticipantes;
    private Integer cuposDisponibles;
    private Boolean activa;

}