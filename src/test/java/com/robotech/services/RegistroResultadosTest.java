package com.robotech.services;

import com.robotech.dto.EnfrentamientoDTO;
import com.robotech.models.*;
import com.robotech.repositories.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ===============================================================
 * PRUEBA 3: REGISTRO DE RESULTADOS - TorneoServiceTest
 * ===============================================================
 * 
 * Prueba el registro de resultados de enfrentamientos.
 * IMPORTANTE: En tu código, registrarResultado está en TorneoService,
 * no en EnfrentamientoService.
 */
@ExtendWith(MockitoExtension.class)
public class RegistroResultadosTest {

    @Mock
    private TorneoRepository torneoRepository;
    
    @Mock
    private EnfrentamientoRepository enfrentamientoRepository;
    
    @Mock
    private ParticipanteRepository participanteRepository;
    
    @Mock
    private RankingService rankingService;
    
    @InjectMocks
    private TorneoService torneoService;
    
    @Test
    public void testRegistrarResultado_DebeActualizarPuntuacionesConVictoria() {
        // =============== ARRANGE ===============
        
        // 1. Preparar torneo
        Torneo torneo = new Torneo();
        torneo.setId(1L);
        torneo.setNombre("Torneo Nacional 2024");
        torneo.setEstado("ACTIVO");
        torneo.setModalidad("ELIMINATORIA");
        
        User juez = new User();
        juez.setId(2L);
        juez.setNombre("María");
        juez.setApellido("Rodríguez");
        torneo.setJuezResponsable(juez);
        
        when(torneoRepository.findById(1L)).thenReturn(Optional.of(torneo));
        
        // 2. Preparar usuarios y robots
        User usuario1 = new User();
        usuario1.setId(1L);
        usuario1.setNombre("Juan");
        usuario1.setApellido("Pérez");
        
        Robot robot1 = new Robot();
        robot1.setId(1L);
        robot1.setNombre("Thunder Bot");
        robot1.setUsuario(usuario1);
        
        User usuario2 = new User();
        usuario2.setId(3L);
        usuario2.setNombre("María");
        usuario2.setApellido("García");
        
        Robot robot2 = new Robot();
        robot2.setId(2L);
        robot2.setNombre("Speed Racer");
        robot2.setUsuario(usuario2);
        
        // 3. Preparar participantes
        Participante participante1 = new Participante();
        participante1.setId(1L);
        participante1.setUsuario(usuario1);
        participante1.setRobot(robot1);
        participante1.setNombreRobot("Thunder Bot");
        participante1.setPuntuacionTotal(0);
        participante1.setPartidosGanados(0);
        participante1.setPartidosPerdidos(0);
        participante1.setPartidosEmpatados(0);
        
        Participante participante2 = new Participante();
        participante2.setId(2L);
        participante2.setUsuario(usuario2);
        participante2.setRobot(robot2);
        participante2.setNombreRobot("Speed Racer");
        participante2.setPuntuacionTotal(0);
        participante2.setPartidosGanados(0);
        participante2.setPartidosPerdidos(0);
        participante2.setPartidosEmpatados(0);
        
        // 4. Preparar enfrentamiento
        Enfrentamiento enfrentamiento = new Enfrentamiento();
        enfrentamiento.setId(1L);
        enfrentamiento.setTorneo(torneo);
        enfrentamiento.setParticipante1(participante1);
        enfrentamiento.setParticipante2(participante2);
        enfrentamiento.setRonda("CUARTOS");
        enfrentamiento.setResultado("PENDIENTE");
        enfrentamiento.setPuntosParticipante1(0);
        enfrentamiento.setPuntosParticipante2(0);
        
        when(enfrentamientoRepository.findById(1L)).thenReturn(Optional.of(enfrentamiento));
        
        // 5. Mock del guardado
        when(enfrentamientoRepository.save(any(Enfrentamiento.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        when(participanteRepository.save(any(Participante.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // =============== ACT ===============
        // Llamar al método real: registrarResultado(torneoId, enfrentamientoId, puntos1, puntos2, juezId)
        EnfrentamientoDTO resultado = torneoService.registrarResultado(
            1L,  // torneoId
            1L,  // enfrentamientoId
            3,   // puntos1
            0,   // puntos2
            2L   // juezId
        );
        
        // =============== ASSERT ===============
        
        // 1. Verificar que el DTO se creó correctamente
        assertNotNull(resultado, "El resultado no debería ser nulo");
        assertEquals(3, resultado.getPuntosParticipante1());
        assertEquals(0, resultado.getPuntosParticipante2());
        assertEquals("GANA_1", resultado.getResultado());
        
        // 2. Verificar actualización del participante ganador
        verify(participanteRepository, times(1)).save(argThat(p ->
            p.getId().equals(1L) &&
            p.getPuntuacionTotal() == 3 &&
            p.getPartidosGanados() == 1 &&
            p.getPartidosPerdidos() == 0
        ));
        
        // 3. Verificar actualización del participante perdedor
        verify(participanteRepository, times(1)).save(argThat(p ->
            p.getId().equals(2L) &&
            p.getPuntuacionTotal() == 0 &&
            p.getPartidosGanados() == 0 &&
            p.getPartidosPerdidos() == 1
        ));
        
        // 4. Verificar que se guardó el enfrentamiento
        verify(enfrentamientoRepository, times(1)).save(any(Enfrentamiento.class));
        
        // 5. Verificar que se limpió el caché del ranking
        verify(rankingService, times(1)).limpiarCacheRanking(1L);
    }
    
    @Test
    public void testRegistrarResultado_DebeDetectarEmpateEnTodosContraTodos() {
        // ARRANGE
        Torneo torneo = new Torneo();
        torneo.setId(1L);
        torneo.setModalidad("TODOS_CONTRA_TODOS"); // Permite empates
        
        User juez = new User();
        juez.setId(2L);
        torneo.setJuezResponsable(juez);
        
        when(torneoRepository.findById(1L)).thenReturn(Optional.of(torneo));
        
        User user1 = new User();
        user1.setId(1L);
        user1.setNombre("Juan");
        user1.setApellido("Pérez");
        
        Participante p1 = new Participante();
        p1.setId(1L);
        p1.setUsuario(user1);
        p1.setPuntuacionTotal(0);
        p1.setPartidosEmpatados(0);
        p1.setNombreRobot("Robot 1");
        
        User user2 = new User();
        user2.setId(3L);
        user2.setNombre("María");
        user2.setApellido("García");
        
        Participante p2 = new Participante();
        p2.setId(2L);
        p2.setUsuario(user2);
        p2.setPuntuacionTotal(0);
        p2.setPartidosEmpatados(0);
        p2.setNombreRobot("Robot 2");
        
        Enfrentamiento enfrentamiento = new Enfrentamiento();
        enfrentamiento.setId(1L);
        enfrentamiento.setTorneo(torneo);
        enfrentamiento.setParticipante1(p1);
        enfrentamiento.setParticipante2(p2);
        enfrentamiento.setResultado("PENDIENTE");
        
        when(enfrentamientoRepository.findById(1L)).thenReturn(Optional.of(enfrentamiento));
        when(enfrentamientoRepository.save(any(Enfrentamiento.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(participanteRepository.save(any(Participante.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // ACT
        EnfrentamientoDTO resultado = torneoService.registrarResultado(
            1L, 1L, 2, 2, 2L  // Empate 2-2
        );
        
        // ASSERT
        assertEquals("EMPATE", resultado.getResultado());
        
        // Verificar que ambos recibieron 1 punto
        verify(participanteRepository, times(1)).save(argThat(p ->
            p.getId().equals(1L) &&
            p.getPuntuacionTotal() == 1 &&
            p.getPartidosEmpatados() == 1
        ));
        
        verify(participanteRepository, times(1)).save(argThat(p ->
            p.getId().equals(2L) &&
            p.getPuntuacionTotal() == 1 &&
            p.getPartidosEmpatados() == 1
        ));
        
        verify(rankingService, times(1)).limpiarCacheRanking(1L);
    }
    
    @Test
    public void testRegistrarResultado_DebeRechazarEmpateEnEliminatoria() {
        // ARRANGE
        Torneo torneo = new Torneo();
        torneo.setId(1L);
        torneo.setModalidad("ELIMINATORIA"); // NO permite empates
        
        User juez = new User();
        juez.setId(2L);
        torneo.setJuezResponsable(juez);
        
        when(torneoRepository.findById(1L)).thenReturn(Optional.of(torneo));
        
        Enfrentamiento enfrentamiento = new Enfrentamiento();
        enfrentamiento.setId(1L);
        enfrentamiento.setResultado("PENDIENTE");
        
        when(enfrentamientoRepository.findById(1L)).thenReturn(Optional.of(enfrentamiento));
        
        // ACT & ASSERT
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> torneoService.registrarResultado(1L, 1L, 2, 2, 2L) // Empate
        );
        
        assertTrue(exception.getMessage().contains("No se permiten empates en modalidad ELIMINATORIA"));
        verify(participanteRepository, never()).save(any(Participante.class));
    }
    
    @Test
    public void testRegistrarResultado_DebeRechazarJuezNoAutorizado() {
        // ARRANGE
        Torneo torneo = new Torneo();
        torneo.setId(1L);
        
        User juezCorrecto = new User();
        juezCorrecto.setId(2L);
        torneo.setJuezResponsable(juezCorrecto);
        
        when(torneoRepository.findById(1L)).thenReturn(Optional.of(torneo));
        
        // ACT & ASSERT - Intenta con un juez diferente
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> torneoService.registrarResultado(1L, 1L, 3, 0, 999L) // Juez ID diferente
        );
        
        assertTrue(exception.getMessage().contains("No eres el juez responsable"));
        verify(enfrentamientoRepository, never()).save(any(Enfrentamiento.class));
    }
    
    @Test
    public void testRegistrarResultado_DebeRechazarEnfrentamientoYaResuelto() {
        // ARRANGE
        Torneo torneo = new Torneo();
        torneo.setId(1L);
        
        User juez = new User();
        juez.setId(2L);
        torneo.setJuezResponsable(juez);
        
        when(torneoRepository.findById(1L)).thenReturn(Optional.of(torneo));
        
        Enfrentamiento enfrentamiento = new Enfrentamiento();
        enfrentamiento.setId(1L);
        enfrentamiento.setResultado("GANA_1"); // Ya tiene resultado
        
        when(enfrentamientoRepository.findById(1L)).thenReturn(Optional.of(enfrentamiento));
        
        // ACT & ASSERT
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> torneoService.registrarResultado(1L, 1L, 3, 0, 2L)
        );
        
        assertTrue(exception.getMessage().contains("ya tiene un resultado registrado"));
        verify(participanteRepository, never()).save(any(Participante.class));
    }
}