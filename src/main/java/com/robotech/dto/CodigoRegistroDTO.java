package com.robotech.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CodigoRegistroDTO {
    private Long id;
    private String codigo;
    private Boolean usado;
    private LocalDateTime fechaGeneracion;
    private LocalDateTime fechaUso;
    private String generadoPorNombre;
    private String usadoPorNombre;
    private String usadoPorEmail;
    private Long clubId;
    private String clubNombre;
}