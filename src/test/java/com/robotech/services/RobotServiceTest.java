package com.robotech.services;

import com.robotech.models.*;
import com.robotech.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ================================================================
 * PRUEBAS UNITARIAS - REGISTRAR Y CONSULTAR ROBOTS (CU13, CU14)
 * ================================================================
 */
@ExtendWith(MockitoExtension.class)
public class RobotServiceTest {

    @Mock
    private RobotRepository robotRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private SimilarityService similarityService;

    @InjectMocks
    private RobotService robotService;

    // ============ DATOS DE PRUEBA ============
    private User usuario;
    private Club club;
    private Categoria categoria;
    private Robot robot;
    private Robot robotExistente;

    @BeforeEach
    void setUp() {
        // Club
        club = new Club();
        club.setId(1L);
        club.setNombre("RoboTech Lima");

        // Usuario aprobado con club
        usuario = new User();
        usuario.setId(1L);
        usuario.setNombre("Carlos");
        usuario.setApellido("Mendoza");
        usuario.setEmail("carlos@gmail.com");
        usuario.setEstado("APROBADO");
        usuario.setClub(club);

        // Categoría
        categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNombre("Ligero");
        categoria.setPesoMaximo(1500);

        // Robot nuevo para registrar
        robot = new Robot();
        robot.setNombre("Destructor 3000");
        robot.setPeso(1200);
        robot.setDescripcion("Robot de combate ligero");

        // Robot existente
        robotExistente = new Robot();
        robotExistente.setId(1L);
        robotExistente.setNombre("Titan Alpha");
        robotExistente.setUsuario(usuario);
        robotExistente.setCategoria(categoria);
        robotExistente.setEstado("PENDIENTE");
    }

    // ============ TESTS ============

    @Test
    void testRegistrarRobot_DebeCrearConDatosValidos() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(robotRepository.findByUsuarioId(1L)).thenReturn(Collections.emptyList());
        when(similarityService.existeRobotSimilar(anyString(), anyList())).thenReturn(false);
        when(robotRepository.save(any(Robot.class))).thenReturn(robot);

        // Act
        Robot resultado = robotService.registrarRobot(robot, 1L, 1L);

        // Assert
        assertNotNull(resultado);
        assertEquals(usuario, resultado.getUsuario());
        assertEquals(categoria, resultado.getCategoria());
        assertEquals("PENDIENTE", resultado.getEstado());
        verify(robotRepository, times(1)).save(any(Robot.class));

        System.out.println("✅ TEST PASADO: Registrar robot con datos válidos");
    }

    @Test
    void testRegistrarRobot_DebeRechazarUsuarioNoAprobado() {
        // Arrange
        usuario.setEstado("PENDIENTE");
        when(userRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> robotService.registrarRobot(robot, 1L, 1L)
        );

        assertTrue(exception.getMessage().contains("cuenta debe estar aprobada"));
        verify(robotRepository, never()).save(any());

        System.out.println("✅ TEST PASADO: Rechazar usuario no aprobado");
    }

    @Test
    void testRegistrarRobot_DebeRechazarUsuarioSinClub() {
        // Arrange
        usuario.setClub(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> robotService.registrarRobot(robot, 1L, 1L)
        );

        assertTrue(exception.getMessage().contains("pertenecer a un club"));
        verify(robotRepository, never()).save(any());

        System.out.println("✅ TEST PASADO: Rechazar usuario sin club");
    }

    @Test
    void testRegistrarRobot_DebeRechazarExcesoDePeso() {
        // Arrange
        robot.setPeso(2000); // Excede el límite de 1500
        when(userRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(robotRepository.findByUsuarioId(1L)).thenReturn(Collections.emptyList());

        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> robotService.registrarRobot(robot, 1L, 1L)
        );

        assertTrue(exception.getMessage().contains("excede el peso máximo"));
        verify(robotRepository, never()).save(any());

        System.out.println("✅ TEST PASADO: Rechazar robot por exceso de peso");
    }

    @Test
    void testRegistrarRobot_DebeRechazarLimite5Robots() {
        // Arrange
        List<Robot> robotsExistentes = Arrays.asList(
            new Robot(), new Robot(), new Robot(), new Robot(), new Robot()
        );
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(robotRepository.findByUsuarioId(1L)).thenReturn(robotsExistentes);

        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> robotService.registrarRobot(robot, 1L, 1L)
        );

        assertTrue(exception.getMessage().contains("límite máximo de 5 robots"));
        verify(robotRepository, never()).save(any());

        System.out.println("✅ TEST PASADO: Rechazar por límite de 5 robots");
    }

    @Test
    void testRegistrarRobot_DebeRechazarNombreSimilar() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(robotRepository.findByUsuarioId(1L)).thenReturn(Arrays.asList(robotExistente));
        when(similarityService.existeRobotSimilar(anyString(), anyList())).thenReturn(true);
        when(similarityService.encontrarRobotSimilar(anyString(), anyList()))
            .thenReturn("Titan Alpha");

        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> robotService.registrarRobot(robot, 1L, 1L)
        );

        assertTrue(exception.getMessage().contains("nombre similar"));
        verify(robotRepository, never()).save(any());

        System.out.println("✅ TEST PASADO: Rechazar nombre de robot similar");
    }

    @Test
    void testConsultarRobotsPorUsuario_DebeRetornarListaDeRobots() {
        // Arrange
        List<Robot> robots = Arrays.asList(robotExistente);
        when(robotRepository.findByUsuarioId(1L)).thenReturn(robots);

        // Act
        List<Robot> resultado = robotService.getRobotsByUsuario(1L);

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Titan Alpha", resultado.get(0).getNombre());
        verify(robotRepository, times(1)).findByUsuarioId(1L);

        System.out.println("✅ TEST PASADO: Consultar robots por usuario");
    }

    @Test
    void testAprobarRobot_DebeActualizarEstadoAAprobado() {
        // Arrange
        Role roleClubOwner = new Role();
        roleClubOwner.setNombre("ROLE_CLUB_OWNER");
        User clubOwner = new User();
        clubOwner.setId(2L);
        clubOwner.setRoles(Set.of(roleClubOwner));

        robotExistente.setEstado("PENDIENTE");

        when(robotRepository.findById(1L)).thenReturn(Optional.of(robotExistente));
        when(userRepository.findById(2L)).thenReturn(Optional.of(clubOwner));
        when(clubRepository.findByOwnerId(2L)).thenReturn(Optional.of(club));
        when(robotRepository.save(any(Robot.class))).thenReturn(robotExistente);

        // Act
        Robot resultado = robotService.aprobarRobot(1L, 2L);

        // Assert
        assertEquals("APROBADO", resultado.getEstado());
        verify(robotRepository, times(1)).save(robotExistente);

        System.out.println("✅ TEST PASADO: Aprobar robot pendiente");
    }

    @Test
    void testEliminarRobot_DebeEliminarRobotPropio() {
        // Arrange
        when(robotRepository.findById(1L)).thenReturn(Optional.of(robotExistente));

        // Act
        robotService.eliminarRobot(1L, 1L);

        // Assert
        verify(robotRepository, times(1)).delete(robotExistente);

        System.out.println("✅ TEST PASADO: Eliminar robot propio");
    }

    @Test
    void testEliminarRobot_DebeRechazarEliminarRobotAjeno() {
        // Arrange
        when(robotRepository.findById(1L)).thenReturn(Optional.of(robotExistente));

        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> robotService.eliminarRobot(1L, 999L) // Usuario diferente
        );

        assertTrue(exception.getMessage().contains("Solo puedes eliminar tus propios robots"));
        verify(robotRepository, never()).delete(any());

        System.out.println("✅ TEST PASADO: Rechazar eliminar robot ajeno");
    }
}