package com.robotech.controllers;

import com.robotech.dto.SolicitudTransferenciaDTO;
import com.robotech.services.TransferenciaService;
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
@RequestMapping("/api/transferencias")
@RequiredArgsConstructor
@Tag(name = "Transferencias", description = "Gestión de transferencias entre clubs e ingresos nuevos")
@SecurityRequirement(name = "bearerAuth")
public class TransferenciaController {

    private final TransferenciaService transferenciaService;
    private final com.robotech.repositories.UserRepository userRepository;

    // ==================== ENDPOINTS PARA USUARIOS SIN CLUB (NUEVOS) ====================

    @Operation(summary = "✅ NUEVO: Solicitar unirse a un club (USER sin club)", 
               description = "Permite a usuarios degradados (sin club) solicitar unirse a un club nuevo")
    @PostMapping("/solicitar-ingreso")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> solicitarIngresoNuevo(
            @RequestBody SolicitarIngresoNuevoRequest request,
            Authentication authentication) {
        try {
            Long usuarioId = getCurrentUserId(authentication);
            
            SolicitudTransferenciaDTO solicitud = transferenciaService.solicitarIngresoNuevo(
                usuarioId, 
                request.getClubDestinoId(), 
                request.getMensaje()
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Solicitud de ingreso enviada. Esperando aprobación del club.",
                "solicitud", solicitud
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    // ==================== ENDPOINTS PARA COMPETIDORES (ORIGINALES) ====================

    @Operation(summary = "✅ Solicitar transferencia a otro club (COMPETITOR)")
    @PostMapping("/solicitar")
    @PreAuthorize("hasRole('COMPETITOR')")
    public ResponseEntity<?> solicitarTransferencia(
            @RequestBody SolicitarTransferenciaRequest request,
            Authentication authentication) {
        try {
            Long usuarioId = getCurrentUserId(authentication);
            
            SolicitudTransferenciaDTO solicitud = transferenciaService.solicitarTransferencia(
                usuarioId, 
                request.getClubDestinoId(), 
                request.getMensaje()
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Solicitud de transferencia enviada. Esperando aprobación del club actual.",
                "solicitud", solicitud
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "📋 Listar mis solicitudes (COMPETITOR o USER)")
    @GetMapping("/mis-solicitudes")
    @PreAuthorize("hasAnyRole('COMPETITOR', 'USER')")
    public ResponseEntity<List<SolicitudTransferenciaDTO>> getMisSolicitudes(Authentication authentication) {
        Long usuarioId = getCurrentUserId(authentication);
        List<SolicitudTransferenciaDTO> solicitudes = transferenciaService.getMisSolicitudes(usuarioId);
        return ResponseEntity.ok(solicitudes);
    }

    @Operation(summary = "❌ Cancelar solicitud (COMPETITOR o USER)")
    @DeleteMapping("/{solicitudId}/cancelar")
    @PreAuthorize("hasAnyRole('COMPETITOR', 'USER')")
    public ResponseEntity<?> cancelarSolicitud(
            @PathVariable Long solicitudId,
            Authentication authentication) {
        try {
            Long usuarioId = getCurrentUserId(authentication);
            transferenciaService.cancelarSolicitud(solicitudId, usuarioId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Solicitud cancelada exitosamente"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    // ==================== ENDPOINTS PARA CLUB OWNERS ====================

    @Operation(summary = "📋 Listar solicitudes de SALIDA de mi club (CLUB_OWNER)")
    @GetMapping("/pendientes-salida")
    @PreAuthorize("hasRole('CLUB_OWNER')")
    public ResponseEntity<List<SolicitudTransferenciaDTO>> getSolicitudesPendientesSalida(
            Authentication authentication) {
        Long clubOwnerId = getCurrentUserId(authentication);
        List<SolicitudTransferenciaDTO> solicitudes = transferenciaService
            .getSolicitudesPendientesSalida(clubOwnerId);
        return ResponseEntity.ok(solicitudes);
    }

    @Operation(summary = "📋 Listar solicitudes de INGRESO a mi club (CLUB_OWNER)", 
               description = "Incluye transferencias de otros clubs e ingresos nuevos de usuarios sin club")
    @GetMapping("/pendientes-ingreso")
    @PreAuthorize("hasRole('CLUB_OWNER')")
    public ResponseEntity<List<SolicitudTransferenciaDTO>> getSolicitudesPendientesIngreso(
            Authentication authentication) {
        Long clubOwnerId = getCurrentUserId(authentication);
        List<SolicitudTransferenciaDTO> solicitudes = transferenciaService
            .getSolicitudesPendientesIngreso(clubOwnerId);
        return ResponseEntity.ok(solicitudes);
    }

    @Operation(summary = "✅ Aprobar/Rechazar SALIDA de competidor (CLUB_OWNER origen)")
    @PutMapping("/{solicitudId}/procesar-salida")
    @PreAuthorize("hasRole('CLUB_OWNER')")
    public ResponseEntity<?> procesarSalida(
            @PathVariable Long solicitudId,
            @RequestBody ProcesarSolicitudRequest request,
            Authentication authentication) {
        try {
            Long clubOwnerId = getCurrentUserId(authentication);
            
            SolicitudTransferenciaDTO solicitud = transferenciaService.procesarSalida(
                solicitudId, 
                clubOwnerId, 
                request.getAprobar(), 
                request.getMotivo()
            );
            
            String mensaje = request.getAprobar() 
                ? "Salida aprobada. Solicitud enviada al club destino." 
                : "Salida rechazada. El competidor permanece en tu club.";
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", mensaje,
                "solicitud", solicitud
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "✅ Aprobar/Rechazar INGRESO (CLUB_OWNER destino)", 
               description = "Procesa tanto transferencias como ingresos nuevos")
    @PutMapping("/{solicitudId}/procesar-ingreso")
    @PreAuthorize("hasRole('CLUB_OWNER')")
    public ResponseEntity<?> procesarIngreso(
            @PathVariable Long solicitudId,
            @RequestBody ProcesarSolicitudRequest request,
            Authentication authentication) {
        try {
            Long clubOwnerId = getCurrentUserId(authentication);
            
            SolicitudTransferenciaDTO solicitud = transferenciaService.procesarIngreso(
                solicitudId, 
                clubOwnerId, 
                request.getAprobar(), 
                request.getMotivo()
            );
            
            String mensaje = request.getAprobar() 
                ? "¡Ingreso aprobado! El usuario ahora pertenece a tu club." 
                : "Ingreso rechazado. El usuario no se unirá a tu club.";
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", mensaje,
                "solicitud", solicitud
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private Long getCurrentUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"))
                .getId();
    }

    // ==================== DTOs ====================

    @Data
    public static class SolicitarTransferenciaRequest {
        private Long clubDestinoId;
        private String mensaje;
    }

    @Data
    public static class SolicitarIngresoNuevoRequest {
        private Long clubDestinoId;
        private String mensaje;
    }

    @Data
    public static class ProcesarSolicitudRequest {
        private Boolean aprobar;
        private String motivo;
    }
}