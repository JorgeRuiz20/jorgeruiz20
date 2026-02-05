package com.robotech.services;

import com.robotech.models.Torneo;
import com.robotech.repositories.TorneoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ✅ MEJORADO: Servicio para activar torneos automáticamente según su fecha programada
 * Se ejecuta cada minuto para verificar si hay torneos que deben activarse
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TorneoScheduler {

    private final TorneoRepository torneoRepository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * ✅ Ejecuta cada minuto: verifica y activa torneos programados
     * Cron: "0 * * * * ?" = cada minuto en el segundo 0
     */
    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void verificarYActivarTorneosProgramados() {
        try {
            List<Torneo> torneosPendientes = torneoRepository.findByEstado("PENDIENTE");
            LocalDateTime ahora = LocalDateTime.now();
            int torneosActivados = 0;

            for (Torneo torneo : torneosPendientes) {
                if (torneo.debeActivarseAutomaticamente()) {
                    log.info("🚀 Activando torneo programado: {} (ID: {})", 
                             torneo.getNombre(), torneo.getId());
                    log.info("   📅 Fecha programada: {}", 
                             torneo.getFechaActivacionProgramada().format(FORMATTER));
                    log.info("   🕐 Fecha actual: {}", ahora.format(FORMATTER));
                    log.info("   🏟️ Sede: {}", 
                             torneo.getSede() != null ? torneo.getSede().getNombre() : "Sin sede");
                    log.info("   🎯 Categoría: {}", 
                             torneo.getCategoria() != null ? torneo.getCategoria().getNombre() : "Sin categoría");
                    
                    torneo.setEstado("ACTIVO");
                    torneo.setFechaInicio(ahora);
                    torneoRepository.save(torneo);
                    
                    torneosActivados++;
                    
                    log.info("   ✅ Torneo activado exitosamente");
                }
            }

            if (torneosActivados > 0) {
                log.info("✅ {} torneo(s) activado(s) automáticamente a las {}", 
                         torneosActivados, ahora.format(FORMATTER));
            }

        } catch (Exception e) {
            log.error("❌ Error en scheduler de torneos: {}", e.getMessage(), e);
        }
    }

    /**
     * ✅ MEJORADO: Muestra torneos próximos a activarse
     * Se ejecuta cada 5 minutos
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void mostrarTorneosProximos() {
        try {
            List<Torneo> torneosPendientes = torneoRepository.findByEstado("PENDIENTE");
            LocalDateTime ahora = LocalDateTime.now();
            LocalDateTime dentroDeUnaHora = ahora.plusHours(1);
            
            List<Torneo> proximosAActivarse = torneosPendientes.stream()
                .filter(t -> Boolean.TRUE.equals(t.getActivacionAutomatica()))
                .filter(t -> t.getFechaActivacionProgramada() != null)
                .filter(t -> t.getFechaActivacionProgramada().isBefore(dentroDeUnaHora))
                .filter(t -> t.getFechaActivacionProgramada().isAfter(ahora))
                .toList();
            
            if (!proximosAActivarse.isEmpty()) {
                log.info("⏰ {} torneo(s) se activarán en la próxima hora:", proximosAActivarse.size());
                for (Torneo t : proximosAActivarse) {
                    long minutosRestantes = java.time.Duration.between(ahora, t.getFechaActivacionProgramada()).toMinutes();
                    log.info("   📌 {} - Sede: {} - En {} minutos ({})", 
                             t.getNombre(),
                             t.getSede() != null ? t.getSede().getNombre() : "Sin sede",
                             minutosRestantes,
                             t.getFechaActivacionProgramada().format(FORMATTER));
                }
            }
            
        } catch (Exception e) {
            log.error("❌ Error mostrando torneos próximos: {}", e.getMessage());
        }
    }

    /**
     * ✅ Limpieza de torneos muy antiguos (opcional)
     * Se ejecuta diariamente a las 3 AM
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void limpiezaDeTorneosAntiguos() {
        try {
            LocalDateTime haceTresAnos = LocalDateTime.now().minusYears(3);
            List<Torneo> torneosAntiguos = torneoRepository.findAll().stream()
                    .filter(t -> "FINALIZADO".equals(t.getEstado()))
                    .filter(t -> t.getFechaFin() != null && t.getFechaFin().isBefore(haceTresAnos))
                    .toList();

            if (!torneosAntiguos.isEmpty()) {
                log.info("🧹 {} torneos finalizados hace más de 3 años detectados", 
                         torneosAntiguos.size());
                // Aquí podrías archivarlos o marcarlos como archivados
            }

        } catch (Exception e) {
            log.error("❌ Error en limpieza de torneos: {}", e.getMessage(), e);
        }
    }

    /**
     * ✅ NUEVO: Log de resumen diario
     * Se ejecuta todos los días a las 8 AM
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void resumenDiario() {
        try {
            long torneosPendientes = torneoRepository.findByEstado("PENDIENTE").size();
            long torneosActivos = torneoRepository.findByEstado("ACTIVO").size();
            long torneosFinalizados = torneoRepository.findByEstado("FINALIZADO").size();
            
            log.info("📊 RESUMEN DIARIO DE TORNEOS - {}", LocalDateTime.now().format(FORMATTER));
            log.info("   ⏳ Pendientes: {}", torneosPendientes);
            log.info("   ✅ Activos: {}", torneosActivos);
            log.info("   🏁 Finalizados: {}", torneosFinalizados);
            
        } catch (Exception e) {
            log.error("❌ Error en resumen diario: {}", e.getMessage());
        }
    }
}