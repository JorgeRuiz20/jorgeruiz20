package com.robotech.controllers;

import com.robotech.dto.ClubDeshabilitacionDTO;
import com.robotech.services.ClubDeshabilitacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/club-deshabilitacion")
@RequiredArgsConstructor
@Tag(name = "Deshabilitación de Clubs", description = "Gestión de clubs deshabilitados (solo ADMIN)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class ClubDeshabilitacionController {

    private final ClubDeshabilitacionService deshabilitacionService;
    private final com.robotech.repositories.UserRepository userRepository;

    @Operation(
        summary = "🚫 PASO 1: Deshabilitar club",
        description = "Inicia el proceso de deshabilitación, notifica a miembros por email"
    )
    @PostMapping("/deshabilitar")
    public ResponseEntity<?> deshabilitarClub(
            @RequestBody DeshabilitarClubRequest request,
            Authentication authentication) {
        try {
            Long adminId = getCurrentUserId(authentication);
            
            ClubDeshabilitacionDTO resultado = deshabilitacionService.deshabilitarClub(
                request.getClubId(),
                adminId,
                request.getMotivo(),
                request.getDiasLimite()
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Club marcado para deshabilitación. Notificaciones enviadas.",
                "deshabilitacion", resultado
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @Operation(
        summary = "📤 PASO 2: Enviar solicitudes masivas a un club",
        description = "Crea solicitudes de transferencia para todos los miembros sin transferencia"
    )
    @PostMapping("/{deshabilitacionId}/solicitudes-masivas")
    public ResponseEntity<?> enviarSolicitudesMasivas(
            @PathVariable Long deshabilitacionId,
            @RequestBody SolicitudesMasivasRequest request,
            Authentication authentication) {
        try {
            Long adminId = getCurrentUserId(authentication);
            
            Map<String, Object> resultado = deshabilitacionService.enviarSolicitudesMasivas(
                deshabilitacionId,
                request.getClubDestinoId(),
                adminId
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", resultado
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @Operation(
        summary = "⬇️ PASO 3: Degradar miembros restantes",
        description = "Quita rol COMPETITOR a usuarios no reubicados y deshabilita el club"
    )
    @PostMapping("/{deshabilitacionId}/degradar-restantes")
    public ResponseEntity<?> degradarMiembrosRestantes(
            @PathVariable Long deshabilitacionId,
            Authentication authentication) {
        try {
            Long adminId = getCurrentUserId(authentication);
            
            Map<String, Object> resultado = deshabilitacionService.degradarMiembrosRestantes(
                deshabilitacionId,
                adminId
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", resultado
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "📋 Listar todas las deshabilitaciones")
    @GetMapping
    public ResponseEntity<List<ClubDeshabilitacionDTO>> listarDeshabilitaciones() {
        return ResponseEntity.ok(deshabilitacionService.listarDeshabilitaciones());
    }

    @Operation(summary = "🔍 Obtener estado de una deshabilitación")
    @GetMapping("/{deshabilitacionId}")
    public ResponseEntity<?> getEstadoDeshabilitacion(@PathVariable Long deshabilitacionId) {
        try {
            ClubDeshabilitacionDTO estado = deshabilitacionService.getEstadoDeshabilitacion(deshabilitacionId);
            return ResponseEntity.ok(estado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "❌ Cancelar deshabilitación")
    @DeleteMapping("/{deshabilitacionId}/cancelar")
    public ResponseEntity<?> cancelarDeshabilitacion(
            @PathVariable Long deshabilitacionId,
            Authentication authentication) {
        try {
            Long adminId = getCurrentUserId(authentication);
            deshabilitacionService.cancelarDeshabilitacion(deshabilitacionId, adminId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Deshabilitación cancelada exitosamente"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    private Long getCurrentUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"))
                .getId();
    }

    // DTOs
    @Data
    public static class DeshabilitarClubRequest {
        private Long clubId;
        private String motivo;
        private Integer diasLimite; // Opcional, default 7 días
    }

    @Data
    public static class SolicitudesMasivasRequest {
        private Long clubDestinoId;
    }
}