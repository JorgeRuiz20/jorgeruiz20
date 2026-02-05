package com.robotech.controllers;

import com.robotech.services.PDFReportService;
import com.robotech.models.User;
import com.robotech.repositories.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
@Tag(name = "Reportes PDF", description = "Generación de reportes en PDF usando vistas MySQL")
@SecurityRequirement(name = "bearerAuth")
public class PDFReportController {

    private final PDFReportService pdfReportService;
    private final UserRepository userRepository;

    // ==================== REPORTES DE TORNEOS ====================

    @Operation(summary = "📄 Reporte completo de un torneo (PDF)", description = "Genera PDF con info del torneo, participantes, ranking y enfrentamientos")
    @GetMapping("/torneos/{torneoId}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'JUDGE', 'CLUB_OWNER')")
    public ResponseEntity<byte[]> generarReporteTorneoPDF(
            @PathVariable Long torneoId,
            Authentication auth) {
        try {
            // ✅ Obtener el ID del usuario autenticado
            String email = auth.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // ✅ Verificar rol
            boolean esAdmin = user.getRoles().stream()
                    .anyMatch(role -> "ROLE_ADMIN".equals(role.getNombre()));
            boolean esJudge = user.getRoles().stream()
                    .anyMatch(role -> "ROLE_JUDGE".equals(role.getNombre()));
            boolean esClubOwner = user.getRoles().stream()
                    .anyMatch(role -> "ROLE_CLUB_OWNER".equals(role.getNombre()));

            byte[] pdfBytes;
            if (esAdmin || esJudge) {
                // ADMIN y JUDGE: acceso total (por ahora)
                pdfBytes = pdfReportService.generarReporteTorneoCompleto(torneoId);
            } else if (esClubOwner) {
                // CLUB_OWNER: solo si su club participa
                pdfBytes = pdfReportService.generarReporteTorneoValidado(torneoId, user.getId());
            } else {
                throw new SecurityException("No tienes permisos para este reporte");
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=torneo_" + torneoId + "_reporte.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);

        } catch (SecurityException e) {
            throw new SecurityException(e.getMessage());
        } catch (RuntimeException e) {
            throw new RuntimeException("Error generando reporte: " + e.getMessage(), e);
        }
    }

    @Operation(summary = "📊 Ranking de torneo (PDF)", description = "Genera PDF solo con el ranking ordenado de participantes")
    @GetMapping("/torneos/{torneoId}/ranking/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'JUDGE', 'CLUB_OWNER', 'COMPETITOR')")
    public ResponseEntity<byte[]> generarReporteRankingPDF(@PathVariable Long torneoId) {
        try {
            byte[] pdfBytes = pdfReportService.generarReporteRanking(torneoId);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=torneo_" + torneoId + "_ranking.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);

        } catch (RuntimeException e) {
            throw new RuntimeException("Error generando reporte: " + e.getMessage(), e);
        }
    }

    @Operation(summary = "📋 Reporte general de todos los torneos (PDF)", description = "Lista todos los torneos con sus estadísticas básicas")
    @GetMapping("/torneos/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> generarReporteTodosLosTorneosPDF() {
        try {
            byte[] pdfBytes = pdfReportService.generarReporteTodosLosTorneos();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=todos_los_torneos.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);

        } catch (RuntimeException e) {
            throw new RuntimeException("Error generando reporte: " + e.getMessage(), e);
        }
    }

    // ==================== REPORTES DE CLUBS ====================

    @Operation(summary = "🏆 Estadísticas completas de un club (PDF)", description = "Genera PDF con stats del club: miembros, robots, victorias, participantes")
    @GetMapping("/clubs/{clubId}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLUB_OWNER')")
    public ResponseEntity<byte[]> generarReporteClubPDF(
            @PathVariable Long clubId,
            Authentication auth) {
        try {
            // ✅ Obtener el ID del usuario autenticado
            String email = auth.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // ✅ Si es ADMIN, puede ver cualquier club
            boolean esAdmin = user.getRoles().stream()
                    .anyMatch(role -> "ROLE_ADMIN".equals(role.getNombre()));

            byte[] pdfBytes;
            if (esAdmin) {
                // ADMIN: acceso total
                pdfBytes = pdfReportService.generarReporteEstadisticasClub(clubId);
            } else {
                // CLUB_OWNER: solo su club (con validación)
                pdfBytes = pdfReportService.generarReporteClubValidado(clubId, user.getId());
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=club_" + clubId + "_estadisticas.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);

        } catch (SecurityException e) {
            throw new SecurityException(e.getMessage());
        } catch (RuntimeException e) {
            throw new RuntimeException("Error generando reporte: " + e.getMessage(), e);
        }
    }

    @Operation(summary = "📋 Reporte general de todos los clubs (PDF)", description = "Lista todos los clubs con sus estadísticas ordenadas por puntuación")
    @GetMapping("/clubs/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> generarReporteTodosLosClubsPDF() {
        try {
            byte[] pdfBytes = pdfReportService.generarReporteTodosLosClubs();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=todos_los_clubs.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);

        } catch (RuntimeException e) {
            throw new RuntimeException("Error generando reporte: " + e.getMessage(), e);
        }
    }

    // ==================== ENDPOINT DE TEST ====================

    @Operation(summary = "✅ Test de servicio de reportes")
    @GetMapping("/test")
    public ResponseEntity<String> testReportes() {
        return ResponseEntity.ok("✅ Servicio de reportes PDF funcionando - " +
                java.time.LocalDateTime.now());
    }
}