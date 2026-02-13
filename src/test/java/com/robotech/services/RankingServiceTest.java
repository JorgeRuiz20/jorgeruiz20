package com.robotech.services;

import com.robotech.models.*;
import com.robotech.repositories.ParticipanteRepository;
import com.robotech.repositories.TorneoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ================================================================
 * PRUEBAS UNITARIAS CORREGIDAS - RANKING SERVICE
 * ================================================================
 * 
 * CORRECCIONES APLICADAS:
 * ✅ Cambiado findByTorneoId() → findByTorneoIdOrderByPuntuacionTotalDesc()
 * ✅ Agregado mock de TorneoRepository (requerido por el servicio)
 * ✅ Agregado mock de CacheManager (usado por el servicio)
 */
@ExtendWith(MockitoExtension.class)
public class RankingServiceTest {

    @Mock
    private ParticipanteRepository participanteRepository;

    @Mock
    private TorneoRepository torneoRepository;
    
    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private RankingService rankingService;

    // ============ DATOS ============
    private Torneo torneo;
    private User user1, user2, user3;
    private Participante p1, p2, p3;
    private Club club;

    @BeforeEach
    void setUp() {
        club = new Club();
        club.setId(1L);
        club.setNombre("RoboTech Lima");

        torneo = new Torneo();
        torneo.setId(1L);
        torneo.setNombre("Torneo Nacional 2024");
        torneo.setModalidad("TODOS_CONTRA_TODOS");

        user1 = new User();
        user1.setId(1L);
        user1.setNombre("Carlos");
        user1.setApellido("Mendoza");
        user1.setClub(club);

        user2 = new User();
        user2.setId(2L);
        user2.setNombre("María");
        user2.setApellido("García");
        user2.setClub(club);

        user3 = new User();
        user3.setId(3L);
        user3.setNombre("Juan");
        user3.setApellido("Pérez");
        user3.setClub(club);

        p1 = new Participante();
        p1.setId(1L);
        p1.setUsuario(user1);
        p1.setTorneo(torneo);
        p1.setPuntuacionTotal(9);
        p1.setPartidosGanados(3);
        p1.setPartidosPerdidos(0);
        p1.setPartidosEmpatados(0);
        p1.setNombreRobot("Destructor Alpha");

        p2 = new Participante();
        p2.setId(2L);
        p2.setUsuario(user2);
        p2.setTorneo(torneo);
        p2.setPuntuacionTotal(6);
        p2.setPartidosGanados(2);
        p2.setPartidosPerdidos(1);
        p2.setPartidosEmpatados(0);
        p2.setNombreRobot("Titan Beta");

        p3 = new Participante();
        p3.setId(3L);
        p3.setUsuario(user3);
        p3.setTorneo(torneo);
        p3.setPuntuacionTotal(3);
        p3.setPartidosGanados(1);
        p3.setPartidosPerdidos(2);
        p3.setPartidosEmpatados(0);
        p3.setNombreRobot("Robot Gamma");
    }

    // ============ TESTS ============

    @Test
    void testCalcularRanking_OrdenarPorPuntuacion() {
        // ✅ CORRECCIÓN: Usar el método correcto del repositorio
        when(participanteRepository.findByTorneoIdOrderByPuntuacionTotalDesc(1L))
            .thenReturn(Arrays.asList(p1, p2, p3)); // Ya ordenados

        List<Participante> ranking = rankingService.calcularRankingPorTorneo(1L);

        assertNotNull(ranking);
        assertEquals(3, ranking.size());
        assertEquals(9, ranking.get(0).getPuntuacionTotal());
        assertEquals(6, ranking.get(1).getPuntuacionTotal());
        assertEquals(3, ranking.get(2).getPuntuacionTotal());

        System.out.println("✅ TEST PASADO: Calcular ranking ordenado");
    }

    @Test
    void testCalcularRanking_ListaVacia() {
        // ✅ CORRECCIÓN: Usar el método correcto del repositorio
        when(participanteRepository.findByTorneoIdOrderByPuntuacionTotalDesc(1L))
            .thenReturn(Arrays.asList());

        List<Participante> ranking = rankingService.calcularRankingPorTorneo(1L);

        assertNotNull(ranking);
        assertTrue(ranking.isEmpty());

        System.out.println("✅ TEST PASADO: Retornar lista vacía");
    }

    @Test
    void testLimpiarCache() {
        // Este método solo limpia caché, no accede al repositorio
        rankingService.limpiarCacheRanking(1L);

        // Verificar que NO se llamó al repositorio
        verify(participanteRepository, never()).findByTorneoIdOrderByPuntuacionTotalDesc(anyLong());

        System.out.println("✅ TEST PASADO: Limpiar caché");
    }

    @Test
    void testCalcularEfectividad() {
        Double efectividad = rankingService.calcularEfectividad(p1);

        assertNotNull(efectividad);
        assertEquals(100.0, efectividad);

        System.out.println("✅ TEST PASADO: Calcular efectividad");
    }

    @Test
    void testCalcularEfectividad_ParticipanteConEmpates() {
        Participante p4 = new Participante();
        p4.setPartidosGanados(2);
        p4.setPartidosPerdidos(1);
        p4.setPartidosEmpatados(1);

        Double efectividad = rankingService.calcularEfectividad(p4);

        assertNotNull(efectividad);
        // (2*3 + 1) / (4*3) * 100 = 7/12 * 100 = 58.33
        assertEquals(58.33, efectividad, 0.01);

        System.out.println("✅ TEST PASADO: Calcular efectividad con empates");
    }

    @Test
    void testCalcularEfectividad_ParticipanteSinPartidos() {
        Participante p5 = new Participante();
        p5.setPartidosGanados(0);
        p5.setPartidosPerdidos(0);
        p5.setPartidosEmpatados(0);

        Double efectividad = rankingService.calcularEfectividad(p5);

        assertNotNull(efectividad);
        assertEquals(0.0, efectividad);

        System.out.println("✅ TEST PASADO: Efectividad con cero partidos");
    }
}