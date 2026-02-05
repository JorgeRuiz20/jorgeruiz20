package com.robotech.controllers;

import com.robotech.models.HistorialTorneo;
import com.robotech.repositories.HistorialTorneoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/historial")
@RequiredArgsConstructor
@Tag(name = "Historial", description = "Consulta de historial de torneos")
public class HistorialController {

    private final HistorialTorneoRepository historialTorneoRepository;

    @Operation(summary = "Obtener historial completo")
    @GetMapping
    public List<HistorialTorneo> getHistorialCompleto() {
        return historialTorneoRepository.findAllByOrderByFechaEventoDesc();
    }

    @Operation(summary = "Obtener historial por torneo")
    @GetMapping("/torneo/{torneoId}")
    public List<HistorialTorneo> getHistorialPorTorneo(@PathVariable Long torneoId) {
        return historialTorneoRepository.findByTorneoId(torneoId);
    }

    @Operation(summary = "Obtener victorias por club")
    @GetMapping("/club/{clubId}")
    public List<HistorialTorneo> getVictoriasPorClub(@PathVariable Long clubId) {
        return historialTorneoRepository.findByClubGanadorId(clubId);
    }

    @Operation(summary = "Obtener historial por usuario")
    @GetMapping("/usuario/{usuarioId}")
    public List<HistorialTorneo> getHistorialPorUsuario(@PathVariable Long usuarioId) {
        return historialTorneoRepository.findHistorialByUsuario(usuarioId);
    }

    @Operation(summary = "Obtener enfrentamientos entre dos usuarios")
    @GetMapping("/enfrentamientos/{usuarioId1}/{usuarioId2}")
    public List<HistorialTorneo> getEnfrentamientosEntreUsuarios(
            @PathVariable Long usuarioId1, 
            @PathVariable Long usuarioId2) {
        return historialTorneoRepository.findEnfrentamientosEntreUsuarios(usuarioId1, usuarioId2);
    }
}