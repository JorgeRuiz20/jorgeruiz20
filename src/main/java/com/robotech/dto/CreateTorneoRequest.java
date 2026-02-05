package com.robotech.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "Request para crear un torneo con sede y programación automática")
public class CreateTorneoRequest {
    
    @NotBlank(message = "El nombre del torneo es obligatorio")
    @Schema(description = "Nombre del torneo", example = "Copa Nacional de Robótica 2025")
    private String nombre;
    
    @Schema(description = "Descripción del torneo (opcional)", example = "Torneo nacional de robots de combate")
    private String descripcion;

    /**
     * ✅ MEJORADO: ID de sede (no texto libre)
     */

    @NotNull(message = "Debes asignar un juez responsable")
    @Schema(description = "ID del juez que será responsable del torneo")
    private Long juezResponsableId;

    @NotNull(message = "La sede es obligatoria")
    @Schema(description = "ID de la sede donde se realizará el torneo", 
            example = "1",
            required = true)
    private Long sedeId;
    
    @NotNull(message = "La categoría es obligatoria")
    @Schema(description = "ID de la categoría del torneo", 
            example = "1",
            required = true)
    private Long categoriaId;
    
    @Pattern(regexp = "PENDIENTE|ACTIVO|FINALIZADO", 
             message = "Estado debe ser: PENDIENTE, ACTIVO o FINALIZADO")
    @Schema(description = "Estado del torneo", 
            example = "PENDIENTE", 
            defaultValue = "PENDIENTE",
            allowableValues = {"PENDIENTE", "ACTIVO", "FINALIZADO"})
    private String estado = "PENDIENTE";

    /**
     * ✅ MEJORADO: Programación automática mejorada
     */
    @Schema(description = "Fecha y hora en que el torneo se activará automáticamente (opcional)", 
            example = "2025-01-15T10:00:00",
            required = false)
    private LocalDateTime fechaActivacionProgramada;

    @Schema(description = "¿Activar automáticamente en la fecha programada?", 
            example = "true", 
            defaultValue = "false",
            required = false)
    private Boolean activacionAutomatica = false;
}