package com.robotech.services;

import com.robotech.models.*;
import com.robotech.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TorneoServiceAdditionalTest {

    @Mock private TorneoRepository torneoRepository;
    @Mock private ParticipanteRepository participanteRepository;
    @Mock private EnfrentamientoRepository enfrentamientoRepository;
    @Mock private RobotRepository robotRepository;
    @Mock private UserRepository userRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private SedeRepository sedeRepository;
    @Mock private HistorialTorneoRepository historialTorneoRepository;
    @Mock private SimilarityService similarityService;
    @Mock private RankingService rankingService;

    @InjectMocks
    private TorneoService torneoService;

    private Torneo torneo;
    private User usuario;
    private Robot robot;
    private Participante participante;
    private Club club;
    private Categoria categoria;
    private Enfrentamiento enfrentamiento;
    private Sede sede;

    @BeforeEach
    void setUp() {

        sede = new Sede();
        sede.setId(1L);
        sede.setNombre("Sede Lima");
        sede.setActiva(true);

        club = new Club();
        club.setId(1L);
        club.setNombre("RoboTech Lima");

        categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNombre("Ligero");
        categoria.setPesoMaximo(1500);
        categoria.setActiva(true);

        torneo = new Torneo();
        torneo.setId(1L);
        torneo.setNombre("Torneo Nacional");
        torneo.setEstado("PENDIENTE");
        torneo.setModalidad("TODOS_CONTRA_TODOS");
        torneo.setCategoria(categoria);
        torneo.setSede(sede);
        torneo.setFechaInicio(LocalDateTime.now().plusDays(10));
        torneo.setFechaFin(LocalDateTime.now().plusDays(15));

        usuario = new User();
        usuario.setId(1L);
        usuario.setEstado("APROBADO");
        usuario.setClub(club);

        robot = new Robot();
        robot.setId(1L);
        robot.setUsuario(usuario);
        robot.setCategoria(categoria);
        robot.setEstado("APROBADO");
        robot.setPeso(1200);

        participante = new Participante();
        participante.setId(1L);
        participante.setUsuario(usuario);
        participante.setRobot(robot);
        participante.setTorneo(torneo);

        enfrentamiento = new Enfrentamiento();
        enfrentamiento.setId(1L);
        enfrentamiento.setTorneo(torneo);
        enfrentamiento.setParticipante1(participante);
        enfrentamiento.setResultado("PENDIENTE");
        enfrentamiento.setFechaEnfrentamiento(LocalDateTime.now().plusDays(11));
    }

    // ================= INSCRIPCIÓN =================

    @Test
    void testInscribirseEnTorneo_DebeRegistrarParticipanteValido() {

        when(torneoRepository.findById(1L)).thenReturn(Optional.of(torneo));
        when(userRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(robotRepository.findById(1L)).thenReturn(Optional.of(robot));
        when(participanteRepository.findByUsuarioIdAndTorneoId(1L,1L))
                .thenReturn(Optional.empty());
        when(participanteRepository.save(any())).thenReturn(participante);

        Participante result = torneoService.unirseATorneo(1L,1L,1L);

        assertNotNull(result);
        verify(participanteRepository).save(any());
    }

    @Test
    void testInscribirseEnTorneo_DebeRechazarInscripcionDuplicada() {

        when(torneoRepository.findById(1L)).thenReturn(Optional.of(torneo));
        when(userRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(robotRepository.findById(1L)).thenReturn(Optional.of(robot));
        when(participanteRepository.findByUsuarioIdAndTorneoId(1L,1L))
                .thenReturn(Optional.of(participante));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> torneoService.unirseATorneo(1L,1L,1L));

        assertTrue(ex.getMessage().toLowerCase().contains("inscrito"));
        verify(participanteRepository, never()).save(any());
    }

    @Test
    void testInscribirseEnTorneo_DebeRechazarTorneoFinalizado() {

        torneo.setEstado("FINALIZADO");
        when(torneoRepository.findById(1L)).thenReturn(Optional.of(torneo));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> torneoService.unirseATorneo(1L,1L,1L));

        assertTrue(ex.getMessage().toLowerCase().contains("final"));
    }

    @Test
    void testInscribirseEnTorneo_DebeRechazarRobotNoAprobado() {

        robot.setEstado("PENDIENTE");

        when(torneoRepository.findById(1L)).thenReturn(Optional.of(torneo));
        when(userRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(robotRepository.findById(1L)).thenReturn(Optional.of(robot));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> torneoService.unirseATorneo(1L,1L,1L));

        assertTrue(ex.getMessage().toLowerCase().contains("aprobado"));
        verify(participanteRepository, never()).save(any());
    }

    @Test
    void testInscribirseEnTorneo_DebeRechazarCategoriaIncorrecta() {

        Categoria otra = new Categoria();
        otra.setId(2L);
        robot.setCategoria(otra);

        when(torneoRepository.findById(1L)).thenReturn(Optional.of(torneo));
        when(userRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(robotRepository.findById(1L)).thenReturn(Optional.of(robot));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> torneoService.unirseATorneo(1L,1L,1L));

        assertTrue(ex.getMessage().toLowerCase().contains("categor"));
        verify(participanteRepository, never()).save(any());
    }

    // ================= ENFRENTAMIENTOS =================

    @Test
    void testGenerarEnfrentamientos_OK() {

        torneo.setEstado("ACTIVO");

        User juez = new User();
        juez.setId(2L);
        torneo.setJuezResponsable(juez);

        Participante p2 = new Participante();
        p2.setId(2L);

        when(torneoRepository.findById(1L)).thenReturn(Optional.of(torneo));
        when(participanteRepository.findByTorneoIdOrderByPuntuacionTotalDesc(1L))
                .thenReturn(Arrays.asList(participante,p2));
        when(enfrentamientoRepository.save(any())).thenReturn(enfrentamiento);

        Map<String,Object> result = torneoService.generarEnfrentamientos(1L,2L);

        assertNotNull(result);
        verify(enfrentamientoRepository, atLeastOnce()).save(any());
    }

    @Test
    void testGenerarEnfrentamientos_TorneoNoActivo() {

        torneo.setEstado("PENDIENTE");
        when(torneoRepository.findById(1L)).thenReturn(Optional.of(torneo));

        assertThrows(RuntimeException.class,
                () -> torneoService.generarEnfrentamientos(1L,2L));
    }

    // ================= CONSULTAS =================

    @Test
    void testConsultarEnfrentamientos() {

        when(enfrentamientoRepository.findByTorneoId(1L))
                .thenReturn(List.of(enfrentamiento));

        List<Enfrentamiento> result =
                enfrentamientoRepository.findByTorneoId(1L);

        assertEquals(1,result.size());
    }

    @Test
    void testGetAllTorneos() {

        Torneo t2 = new Torneo();
        t2.setId(2L);

        when(torneoRepository.findAll())
                .thenReturn(Arrays.asList(torneo,t2));

        List<Torneo> result = torneoService.getAllTorneos();

        assertEquals(2,result.size());
    }

    @Test
    void testGetTorneosActivos() {

        torneo.setEstado("ACTIVO");
        when(torneoRepository.findByEstado("ACTIVO"))
                .thenReturn(List.of(torneo));

        List<Torneo> result = torneoService.getTorneosActivos();

        assertEquals(1,result.size());
    }

    @Test
    void testGetParticipantesByTorneo() {

        when(torneoRepository.findById(1L)).thenReturn(Optional.of(torneo));
        when(participanteRepository.findByTorneoIdOrderByPuntuacionTotalDesc(1L))
                .thenReturn(List.of(participante));

        List<Participante> result =
                torneoService.getParticipantesByTorneo(1L);

        assertEquals(1,result.size());
    }
}
