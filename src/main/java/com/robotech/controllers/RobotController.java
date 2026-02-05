package com.robotech.controllers;

import com.robotech.dto.RobotDTO;
import com.robotech.models.Robot;
import com.robotech.services.RobotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/robots")
@RequiredArgsConstructor
@Tag(name = "Robots", description = "CRUD completo de robots de competencia")
@SecurityRequirement(name = "bearerAuth")
public class RobotController {

    private final RobotService robotService;
    private final com.robotech.repositories.UserRepository userRepository;

    // ==================== CRUD PARA COMPETIDOR ====================

    @Operation(summary = "✅ Crear nuevo robot (COMPETITOR)")
    @PostMapping
    @PreAuthorize("hasRole('COMPETITOR')")
    public ResponseEntity<?> crearRobot(@Valid @RequestBody CrearRobotRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long usuarioId = getCurrentUserId(authentication);

            Robot robot = new Robot();
            robot.setNombre(request.getNombreRobot());
            robot.setDescripcion(request.getDescripcionRobot());
            robot.setPeso(request.getPeso());
            robot.setEspecificacionesTecnicas(request.getEspecificacionesTecnicas());
            robot.setFotoRobot(request.getFotoRobot());

            Robot savedRobot = robotService.registrarRobot(robot, usuarioId, request.getCategoriaId());
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Robot creado exitosamente",
                "robot", convertToDTO(savedRobot)
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "📋 Listar MIS robots (COMPETITOR)")
    @GetMapping("/mis-robots")
    @PreAuthorize("hasRole('COMPETITOR')")
    public ResponseEntity<List<RobotDTO>> getMisRobots() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long usuarioId = getCurrentUserId(authentication);
        List<Robot> robots = robotService.getRobotsByUsuario(usuarioId);
        return ResponseEntity.ok(robots.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }

    @Operation(summary = "🔍 Obtener robot por ID (COMPETITOR)")
    @GetMapping("/{robotId}")
    @PreAuthorize("hasRole('COMPETITOR')")
    public ResponseEntity<?> getRobotById(@PathVariable Long robotId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long usuarioId = getCurrentUserId(authentication);
            
            Robot robot = robotService.getRobotById(robotId);
            
            // Verificar que el robot pertenece al usuario
            if (!robot.getUsuario().getId().equals(usuarioId)) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "No tienes permisos para ver este robot"
                ));
            }
            
            return ResponseEntity.ok(convertToDTO(robot));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "✏️ Actualizar MI robot (COMPETITOR)")
    @PutMapping("/{robotId}")
    @PreAuthorize("hasRole('COMPETITOR')")
    public ResponseEntity<?> actualizarRobot(
            @PathVariable Long robotId,
            @Valid @RequestBody ActualizarRobotRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long usuarioId = getCurrentUserId(authentication);
            
            Robot robot = robotService.actualizarRobot(robotId, usuarioId, request);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Robot actualizado exitosamente",
                "robot", convertToDTO(robot)
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "🗑️ Eliminar MI robot (COMPETITOR)")
    @DeleteMapping("/{robotId}")
    @PreAuthorize("hasRole('COMPETITOR')")
    public ResponseEntity<?> eliminarRobot(@PathVariable Long robotId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long usuarioId = getCurrentUserId(authentication);
            
            robotService.eliminarRobot(robotId, usuarioId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Robot eliminado exitosamente"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    // ==================== ENDPOINTS PARA CLUB OWNER ====================

    @Operation(summary = "✅ Aprobar robot (CLUB_OWNER)")
    @PutMapping("/{robotId}/aprobar")
    @PreAuthorize("hasRole('CLUB_OWNER')")
    public ResponseEntity<?> aprobarRobot(@PathVariable Long robotId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long clubOwnerId = getCurrentUserId(authentication);
            Robot robot = robotService.aprobarRobot(robotId, clubOwnerId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Robot aprobado exitosamente",
                "robot", convertToDTO(robot)
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "❌ Rechazar robot (CLUB_OWNER)")
    @PutMapping("/{robotId}/rechazar")
    @PreAuthorize("hasRole('CLUB_OWNER')")
    public ResponseEntity<?> rechazarRobot(
            @PathVariable Long robotId,
            @RequestBody RechazarRobotRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long clubOwnerId = getCurrentUserId(authentication);
            Robot robot = robotService.rechazarRobot(robotId, clubOwnerId, request.getMotivo());
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Robot rechazado",
                "robot", convertToDTO(robot)
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "📋 Obtener robots de mi club (CLUB_OWNER)")
    @GetMapping("/mi-club")
    @PreAuthorize("hasRole('CLUB_OWNER')")
    public ResponseEntity<List<RobotDTO>> getRobotsDeMiClub() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long clubOwnerId = getCurrentUserId(authentication);
        List<Robot> robots = robotService.getRobotsByClubOwner(clubOwnerId);
        return ResponseEntity.ok(robots.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }

    @Operation(summary = "⏳ Obtener robots pendientes de mi club (CLUB_OWNER)")
    @GetMapping("/mi-club/pendientes")
    @PreAuthorize("hasRole('CLUB_OWNER')")
    public ResponseEntity<List<RobotDTO>> getRobotsPendientesDeMiClub() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long clubOwnerId = getCurrentUserId(authentication);
        List<Robot> robots = robotService.getRobotsPendientesByClubOwner(clubOwnerId);
        return ResponseEntity.ok(robots.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }

    // ==================== ENDPOINTS PÚBLICOS ====================

    @Operation(summary = "🔍 Obtener robots por categoría")
    @GetMapping("/categoria/{categoriaId}")
    public ResponseEntity<List<RobotDTO>> getRobotsByCategoria(@PathVariable Long categoriaId) {
        List<Robot> robots = robotService.getRobotsAprobadosByCategoria(categoriaId);
        return ResponseEntity.ok(robots.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private Long getCurrentUserId(Authentication authentication) {
        String email = authentication.getName();
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return user.getId();
    }

 // SOLO LA FUNCIÓN convertToDTO actualizada - agregar en tu RobotController.java

    private RobotDTO convertToDTO(Robot robot) {
        RobotDTO dto = new RobotDTO();
        dto.setId(robot.getId());
        dto.setNombre(robot.getNombre());
        dto.setDescripcion(robot.getDescripcion());
        dto.setFotoRobot(robot.getFotoRobot());
        dto.setPeso(robot.getPeso());
        dto.setEspecificacionesTecnicas(robot.getEspecificacionesTecnicas());
        dto.setCodigoIdentificacion(robot.getCodigoIdentificacion());
        dto.setEstado(robot.getEstado());
        
        // ✅ INCLUIR MOTIVO DE RECHAZO
        dto.setMotivoRechazo(robot.getMotivoRechazo());

        if (robot.getUsuario() != null) {
            dto.setUsuarioId(robot.getUsuario().getId());
            dto.setUsuarioNombre(robot.getUsuario().getNombre() + " " + robot.getUsuario().getApellido());

            if (robot.getUsuario().getClub() != null) {
                dto.setClubId(robot.getUsuario().getClub().getId());
                dto.setClubNombre(robot.getUsuario().getClub().getNombre());
            }
        }

        if (robot.getCategoria() != null) {
            dto.setCategoriaId(robot.getCategoria().getId());
            dto.setCategoriaNombre(robot.getCategoria().getNombre());
        }

        return dto;
    }

    // ==================== DTOs ====================

    @Data
    public static class CrearRobotRequest {
        private String nombreRobot;
        private String descripcionRobot;
        private String fotoRobot;
        private Integer peso;
        private String especificacionesTecnicas;
        private Long categoriaId;
    }

    @Data
    public static class ActualizarRobotRequest {
        private String nombreRobot;
        private String descripcionRobot;
        private String fotoRobot;
        private Integer peso;
        private String especificacionesTecnicas;
        private Long categoriaId;
    }

    @Data
    public static class RechazarRobotRequest {
        private String motivo;
    }
}