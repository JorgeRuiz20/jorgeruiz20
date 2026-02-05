package com.robotech.services;

import com.robotech.models.Participante;
import com.robotech.repositories.ParticipanteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RankingService {

    private final ParticipanteRepository participanteRepository;
    private final CacheManager cacheManager; // ✅ NUEVO: Para gestión de caché

    /**
     * Calcula el ranking de un torneo
     * ✅ Se mantiene igual, con caché para optimización
     */
    @Cacheable(value = "ranking", key = "'torneo_' + #torneoId")
    public List<Participante> calcularRankingPorTorneo(Long torneoId) {
        try {
            List<Participante> participantes = participanteRepository.findByTorneoIdOrderByPuntuacionTotalDesc(torneoId);
            
            if (participantes == null || participantes.isEmpty()) {
                return List.of();
            }
            
            return participantes.stream()
                    .sorted(Comparator
                            .comparing(Participante::getPuntuacionTotal, Comparator.nullsFirst(Comparator.naturalOrder())).reversed()
                            .thenComparing(Participante::getPartidosGanados, Comparator.nullsFirst(Comparator.naturalOrder())).reversed()
                            .thenComparing(Participante::getPartidosEmpatados, Comparator.nullsFirst(Comparator.naturalOrder())).reversed()
                    )
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            throw new RuntimeException("Error al calcular ranking por torneo: " + e.getMessage(), e);
        }
    }

    /**
     * ✅ NUEVO: Limpiar caché del ranking cuando el torneo finaliza
     * Se debe llamar desde TorneoService.cambiarEstadoTorneo() 
     * cuando el estado cambia a FINALIZADO
     */
    @CacheEvict(value = "ranking", key = "'torneo_' + #torneoId")
    public void limpiarCacheRanking(Long torneoId) {
        // El caché se limpia automáticamente con la anotación @CacheEvict
        // Esto evita que se siga mostrando ranking en tiempo real de torneos finalizados
    }

    /**
     * Calcula la efectividad de un participante
     * Fórmula: (Ganados * 3 + Empatados) / (Total Partidos * 3) * 100
     * ✅ Se mantiene igual
     */
    public Double calcularEfectividad(Participante participante) {
        try {
            if (participante == null) return 0.0;
            
            Integer ganados = participante.getPartidosGanados() != null ? participante.getPartidosGanados() : 0;
            Integer perdidos = participante.getPartidosPerdidos() != null ? participante.getPartidosPerdidos() : 0;
            Integer empatados = participante.getPartidosEmpatados() != null ? participante.getPartidosEmpatados() : 0;
            
            int totalPartidos = ganados + perdidos + empatados;
            
            if (totalPartidos == 0) return 0.0;
            
            return (double) (ganados * 3 + empatados) / (totalPartidos * 3) * 100;
            
        } catch (Exception e) {
            return 0.0;
        }
    }
}