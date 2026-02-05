package com.robotech.controllers;

import com.robotech.models.Sede;
import com.robotech.services.SedeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sedes")
@RequiredArgsConstructor
@Tag(name = "Sedes", description = "Gestión de sedes para torneos")
@SecurityRequirement(name = "bearerAuth")
public class SedeController {

    private final SedeService sedeService;

    @Operation(summary = "Obtener sedes activas (público)")
    @GetMapping
    public ResponseEntity<List<Sede>> getSedesActivas() {
        return ResponseEntity.ok(sedeService.getSedesActivas());
    }

    @Operation(summary = "Obtener sede por ID (público)")
    @GetMapping("/{id}")
    public ResponseEntity<Sede> getSedeById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(sedeService.getSedeById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Crear sede (solo ADMIN)")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> crearSede(@RequestBody Sede sede) {
        try {
            Sede nuevaSede = sedeService.crearSede(sede);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Sede creada exitosamente",
                "sede", nuevaSede
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Actualizar sede (solo ADMIN)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> actualizarSede(@PathVariable Long id, @RequestBody Sede sede) {
        try {
            Sede sedeActualizada = sedeService.actualizarSede(id, sede);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Sede actualizada exitosamente",
                "sede", sedeActualizada
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Desactivar sede (solo ADMIN)")
    @PutMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> desactivarSede(@PathVariable Long id) {
        try {
            Sede sede = sedeService.desactivarSede(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Sede desactivada exitosamente",
                "sede", sede
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Eliminar sede (solo ADMIN)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> eliminarSede(@PathVariable Long id) {
        try {
            sedeService.eliminarSede(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Sede eliminada exitosamente"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Obtener sedes por distrito")
    @GetMapping("/distrito/{distrito}")
    public ResponseEntity<List<Sede>> getSedesByDistrito(@PathVariable String distrito) {
        return ResponseEntity.ok(sedeService.getSedesByDistrito(distrito));
    }
}