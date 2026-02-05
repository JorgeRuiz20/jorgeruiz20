package com.robotech.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EnfrentamientoDTO {
    private Long id;
    private String participante1Nombre;
    private String participante2Nombre;
    private String participante1Robot;
    private String participante2Robot;
    private Integer puntosParticipante1;
    private Integer puntosParticipante2;
    private String resultado;
    private String ronda;
    private LocalDateTime fechaEnfrentamiento;
    private String juezNombre;
    private Long torneoId;
    private String torneoNombre;
}