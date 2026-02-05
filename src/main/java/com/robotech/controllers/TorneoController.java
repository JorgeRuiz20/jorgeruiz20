package com.robotech.controllers;

import com.robotech.dto.*;
import com.robotech.models.Participante;
import com.robotech.models.Torneo;
import com.robotech.services.TorneoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/torneos")
@RequiredArgsConstructor
@Tag(name = "Torneos", description = "Gestión de torneos")
@SecurityRequirement(name = "bearerAuth")
public class TorneoController {

    private final TorneoService torneoService;
    private final com.robotech.services.RankingService rankingService;
    private final com.robotech.repositories.UserRepository userRepository;

    @Operation(summary = "Obtener todos los torneos")
    @GetMapping
    public List<TorneoDTO> getAllTorneos() {
        return torneoService.getAllTorneos().stream()
                .map(torneoService::convertToDTO)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Obtener torneos activos")
    @GetMapping("/activos")
    public List<TorneoDTO> getTorneosActivos() {
        return torneoService.getTorneosActivos().stream()
                .map(torneoService::convertToDTO)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Obtener torneos pendientes")
    @GetMapping("/pendientes")
    public List<TorneoDTO> getTorneosPendientes() {
        return torneoService.getTorneosPendientes().stream()
                .map(torneoService::convertToDTO)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Obtener torneos finalizados")
    @GetMapping("/finalizados")
    public List<TorneoDTO> getTorneosFinalizados() {
        return torneoService.getTorneosFinalizados().stream()
                .map(torneoService::convertToDTO)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Obtener torneo por ID")
    @GetMapping("/{id}")
    public ResponseEntity<TorneoDTO> getTorneoById(@PathVariable Long id) {
        return torneoService.getTorneoById(id)
                .map(torneo -> ResponseEntity.ok(torneoService.convertToDTO(torneo)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Obtener torneos por categoría")
    @GetMapping("/categoria/{categoriaId}")
    public List<TorneoDTO> getTorneosByCategoria(@PathVariable Long categoriaId) {
        return torneoService.getTorneosByCategoria(categoriaId).stream()
                .map(torneoService::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * ✅ MEJORADO: Validación completa de sede
     */
    @Operation(summary = "Crear torneo (solo ADMIN)", description = "Crea un torneo seleccionando una sede existente y configurando activación automática")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createTorneo(@Valid @RequestBody CreateTorneoRequest request) {
        try {
            Torneo torneo = torneoService.createTorneo(request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Torneo creado exitosamente",
                    "torneo", torneoService.convertToDTO(torneo)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    @Operation(summary = "Actualizar torneo (solo ADMIN)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateTorneo(@PathVariable Long id, @Valid @RequestBody CreateTorneoRequest request) {
        try {
            Torneo torneo = torneoService.updateTorneo(id, request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Torneo actualizado exitosamente",
                    "torneo", torneoService.convertToDTO(torneo)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    @Operation(summary = "Cambiar estado de torneo (solo ADMIN)")
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String nuevoEstado = body.get("estado");
            Torneo torneo = torneoService.cambiarEstadoTorneo(id, nuevoEstado);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Estado actualizado",
                    "torneo", torneoService.convertToDTO(torneo)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    @Operation(summary = "Eliminar torneo (solo ADMIN)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteTorneo(@PathVariable Long id) {
        try {
            torneoService.deleteTorneo(id);
            return ResponseEntity.ok(Map.of("message", "Torneo eliminado exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============ ENDPOINTS PARA ADMIN (NUEVOS) ============

    @Operation(summary = "Iniciar torneo (solo ADMIN)", description = "El admin inicia el torneo poniéndolo en estado ACTIVO. Luego el juez asigna modalidad.")
    @PostMapping("/{torneoId}/iniciar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> iniciarTorneo(@PathVariable Long torneoId) {
        try {
            Map<String, Object> resultado = torneoService.iniciarTorneo(torneoId);
            return ResponseEntity.ok(resultado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============ ENDPOINTS PARA JUEZ (MODIFICADOS) ============
    /**
     * ✅ MODIFICADO: Ya no asigna juez (se asigna al crear torneo)
     * Solo permite al juez ASIGNADO cambiar la modalidad
     */
    @Operation(summary = "Asignar modalidad al torneo (solo JUEZ ASIGNADO)", description = "El juez ASIGNADO elige: ELIMINATORIA o TODOS_CONTRA_TODOS en torneos ACTIVOS")
    @PostMapping("/{torneoId}/asignar-modalidad")
    @PreAuthorize("hasRole('JUDGE')")
    public ResponseEntity<?> asignarModalidad(
            @PathVariable Long torneoId,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        try {
            Long juezId = getCurrentUserId(authentication);
            String modalidad = body.get("modalidad");

            Torneo torneo = torneoService.asignarModalidad(torneoId, modalidad, juezId);

            return ResponseEntity.ok(Map.of(
                    "message", "Modalidad asignada exitosamente",
                    "torneo", torneoService.convertToDTO(torneo),
                    "juezResponsable", torneo.getJuezResponsable().getNombre() + " " +
                            torneo.getJuezResponsable().getApellido(),
                    "siguiente", "Ahora debes generar los enfrentamientos con POST /torneos/" + torneoId
                            + "/generar-enfrentamientos"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "sugerencia", "Verifica que seas el juez asignado a este torneo"));
        }
    }

    @Operation(summary = "Generar enfrentamientos (solo JUEZ)", description = "Genera los enfrentamientos según la modalidad elegida")
    @PostMapping("/{torneoId}/generar-enfrentamientos")
    @PreAuthorize("hasRole('JUDGE')")
    public ResponseEntity<?> generarEnfrentamientos(
            @PathVariable Long torneoId,
            Authentication authentication) {
        try {
            Long juezId = getCurrentUserId(authentication);
            Map<String, Object> resultado = torneoService.generarEnfrentamientos(torneoId, juezId);
            return ResponseEntity.ok(resultado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Obtener enfrentamientos del torneo (JUEZ)")
    @GetMapping("/{torneoId}/enfrentamientos")
    @PreAuthorize("hasRole('JUDGE')")
    public ResponseEntity<?> getEnfrentamientosTorneo(@PathVariable Long torneoId) {
        try {
            List<EnfrentamientoDTO> enfrentamientos = torneoService.getEnfrentamientosTorneo(torneoId);
            return ResponseEntity.ok(enfrentamientos);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Registrar resultado de enfrentamiento (JUEZ)")
    @PutMapping("/{torneoId}/enfrentamientos/{enfrentamientoId}/resultado")
    @PreAuthorize("hasRole('JUDGE')")
    public ResponseEntity<?> registrarResultado(
            @PathVariable Long torneoId,
            @PathVariable Long enfrentamientoId,
            @RequestBody RegistrarResultadoRequest request,
            Authentication authentication) {
        try {
            Long juezId = getCurrentUserId(authentication);
            EnfrentamientoDTO resultado = torneoService.registrarResultado(
                    torneoId, enfrentamientoId, request.getPuntos1(), request.getPuntos2(), juezId);
            return ResponseEntity.ok(resultado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Avanzar ganadores a siguiente fase (JUEZ)")
    @PostMapping("/{torneoId}/avanzar-ganadores")
    @PreAuthorize("hasRole('JUDGE')")
    public ResponseEntity<?> avanzarGanadores(
            @PathVariable Long torneoId,
            Authentication authentication) {
        try {
            Long juezId = getCurrentUserId(authentication);

            Torneo torneo = torneoService.getTorneoById(torneoId)
                    .orElseThrow(() -> new RuntimeException("Torneo no encontrado"));

            if (torneo.getJuezResponsable() == null ||
                    !torneo.getJuezResponsable().getId().equals(juezId)) {
                return ResponseEntity.status(403).body(Map.of(
                        "error", "No eres el juez responsable de este torneo"));
            }

            Map<String, Object> resultado = torneoService.avanzarGanadores(torneoId);
            return ResponseEntity.ok(resultado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============ ENDPOINTS PARA COMPETITOR ============

    @Operation(summary = "Unirse a torneo (COMPETITOR)")
    @PostMapping("/{torneoId}/unirse")
    @PreAuthorize("hasRole('COMPETITOR')")
    public ResponseEntity<?> unirseATorneo(
            @PathVariable Long torneoId,
            @RequestBody UnirseRequest request,
            Authentication authentication) {
        try {
            Long usuarioId = getCurrentUserId(authentication);
            Participante participante = torneoService.unirseATorneo(torneoId, usuarioId, request.getRobotId());
            return ResponseEntity.ok(convertToParticipanteDTO(participante));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Obtener participantes del torneo")
    @GetMapping("/{torneoId}/participantes")
    public List<ParticipanteDTO> getParticipantes(@PathVariable Long torneoId) {
        return torneoService.getParticipantesByTorneo(torneoId).stream()
                .map(this::convertToParticipanteDTO)
                .collect(Collectors.toList());
    }

    // ✅ REEMPLAZAR EL MÉTODO getRanking() EN TorneoController.java
// Ubicación: línea 3093-3129

@Operation(summary = "Obtener ranking del torneo en tiempo real")
@GetMapping("/{torneoId}/ranking")
public ResponseEntity<?> getRanking(@PathVariable Long torneoId) {
    try {
        // ✅ SIMPLE: Delegar toda la lógica al servicio
        // El servicio maneja la transacción y evita lazy loading errors
        Map<String, Object> ranking = torneoService.obtenerRankingCompleto(torneoId);
        return ResponseEntity.ok(ranking);
    } catch (Exception e) {
        System.err.println("❌ Error en getRanking: " + e.getMessage());
        e.printStackTrace();
        return ResponseEntity.badRequest().body(Map.of(
            "error", "Error al obtener el ranking: " + e.getMessage(),
            "estadoTorneo", "ERROR",
            "ranking", List.of()
        ));
    }
}

    @Operation(summary = "Obtener bracket visual del torneo")
    @GetMapping("/{torneoId}/bracket")
    public ResponseEntity<?> obtenerBracket(@PathVariable Long torneoId) {
        try {
            Map<String, Object> bracket = torneoService.obtenerEstadoBracket(torneoId);
            return ResponseEntity.ok(bracket);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============ MÉTODOS AUXILIARES ============

    private Long getCurrentUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"))
                .getId();
    }

    private ParticipanteDTO convertToParticipanteDTO(Participante p) {
        ParticipanteDTO dto = new ParticipanteDTO();
        dto.setId(p.getId());
        dto.setNombreRobot(p.getNombreRobot());
        dto.setDescripcionRobot(p.getDescripcionRobot());
        dto.setPuntuacionTotal(p.getPuntuacionTotal() != null ? p.getPuntuacionTotal() : 0);
        dto.setPartidosGanados(p.getPartidosGanados() != null ? p.getPartidosGanados() : 0);
        dto.setPartidosPerdidos(p.getPartidosPerdidos() != null ? p.getPartidosPerdidos() : 0);
        dto.setPartidosEmpatados(p.getPartidosEmpatados() != null ? p.getPartidosEmpatados() : 0);
        dto.setEfectividad(rankingService.calcularEfectividad(p));
        dto.setUsuarioId(p.getUsuario().getId());
        dto.setUsuarioNombre(p.getUsuario().getNombre() + " " + p.getUsuario().getApellido());
        dto.setUsuarioEmail(p.getUsuario().getEmail());
        dto.setUsuarioDni(p.getUsuario().getDni());

        if (p.getUsuario().getClub() != null) {
            dto.setClubId(p.getUsuario().getClub().getId());
            dto.setClubNombre(p.getUsuario().getClub().getNombre());
        }

        if (p.getRobot() != null && p.getRobot().getCategoria() != null) {
            dto.setCategoriaId(p.getRobot().getCategoria().getId());
            dto.setCategoriaNombre(p.getRobot().getCategoria().getNombre());
        }

        return dto;
    }

    @Data
    public static class UnirseRequest {
        private Long robotId;
    }

    @Data
    public static class RegistrarResultadoRequest {
        private Integer puntos1;
        private Integer puntos2;
    }
}