package com.robotech.services;

import com.robotech.dto.CreateTorneoRequest;
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
 * PRUEBA 2: CREACIÓN DE TORNEO - TorneoServiceTest
 * ===============================================================
 * 
 * Prueba la creación de torneos con todas sus validaciones.
 */
@ExtendWith(MockitoExtension.class)
public class TorneoServiceTest {

    @Mock
    private TorneoRepository torneoRepository;
    
    @Mock
    private CategoriaRepository categoriaRepository;
    
    @Mock
    private SedeRepository sedeRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private TorneoService torneoService;
    
    @Test
    public void testCrearTorneo_DebeFuncionarConDatosCompletos() {
        // =============== ARRANGE ===============
        
        // 1. Preparar request (basado en tu CreateTorneoRequest.java real)
        CreateTorneoRequest request = new CreateTorneoRequest();
        request.setNombre("Torneo Nacional 2024");
        request.setDescripcion("Campeonato nacional de robótica");
        request.setCategoriaId(1L);
        request.setSedeId(1L);
        request.setJuezResponsableId(2L);
        request.setEstado("PENDIENTE");
        request.setFechaActivacionProgramada(LocalDateTime.now().plusDays(7));
        request.setActivacionAutomatica(false);
        
        // 2. Mock de la categoría
        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNombre("Mini Sumo");
        categoria.setPesoMaximo(500);
        categoria.setEdadMinima(12);
        categoria.setEdadMaxima(18);
        categoria.setActiva(true);
        
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        
        // 3. Mock de la sede
        Sede sede = new Sede();
        sede.setId(1L);
        sede.setNombre("Estadio Nacional");
        sede.setDistrito("Lima");
        sede.setDireccion("Av. Arenales 123");
        sede.setCapacidadMaxima(100);
        sede.setActiva(true);
        
        when(sedeRepository.findById(1L)).thenReturn(Optional.of(sede));
        
        // 4. Mock del juez responsable
        User juez = new User();
        juez.setId(2L);
        juez.setNombre("María");
        juez.setApellido("Rodríguez");
        juez.setEmail("maria.rodriguez@robotech.com");
        
        Role roleJudge = new Role();
        roleJudge.setNombre("ROLE_JUDGE");
        juez.setRoles(Set.of(roleJudge));
        
        when(userRepository.findById(2L)).thenReturn(Optional.of(juez));
        
        // 5. Mock del torneo guardado
        Torneo torneoGuardado = new Torneo();
        torneoGuardado.setId(1L);
        torneoGuardado.setNombre("Torneo Nacional 2024");
        torneoGuardado.setDescripcion("Campeonato nacional de robótica");
        torneoGuardado.setCategoria(categoria);
        torneoGuardado.setSede(sede);
        torneoGuardado.setJuezResponsable(juez);
        torneoGuardado.setEstado("PENDIENTE");
        torneoGuardado.setFechaCreacion(LocalDateTime.now());
        torneoGuardado.setFechaActivacionProgramada(request.getFechaActivacionProgramada());
        torneoGuardado.setActivacionAutomatica(false);
        
        when(torneoRepository.save(any(Torneo.class))).thenReturn(torneoGuardado);
        
        // =============== ACT ===============
        Torneo resultado = torneoService.createTorneo(request);
        
        // =============== ASSERT ===============
        
        // 1. Verificar que el torneo se creó correctamente
        assertNotNull(resultado, "El torneo no debería ser nulo");
        assertEquals("Torneo Nacional 2024", resultado.getNombre());
        assertEquals("Campeonato nacional de robótica", resultado.getDescripcion());
        
        // 2. Verificar relaciones
        assertNotNull(resultado.getCategoria());
        assertEquals("Mini Sumo", resultado.getCategoria().getNombre());
        
        assertNotNull(resultado.getSede());
        assertEquals("Estadio Nacional", resultado.getSede().getNombre());
        
        assertNotNull(resultado.getJuezResponsable());
        assertEquals("María Rodríguez", 
            resultado.getJuezResponsable().getNombre() + " " + 
            resultado.getJuezResponsable().getApellido());
        
        // 3. Verificar estado inicial
        assertEquals("PENDIENTE", resultado.getEstado());
        
        // 4. Verificar que se guardó en el repositorio
        verify(torneoRepository, times(1)).save(argThat(torneo ->
            torneo.getNombre().equals("Torneo Nacional 2024") &&
            torneo.getEstado().equals("PENDIENTE") &&
            torneo.getCategoria() != null &&
            torneo.getSede() != null &&
            torneo.getJuezResponsable() != null
        ));
        
        // 5. Verificar que se validaron las relaciones
        verify(categoriaRepository, times(1)).findById(1L);
        verify(sedeRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).findById(2L);
    }
    
    @Test
    public void testCrearTorneo_DebeRechazarCategoriaInexistente() {
        // ARRANGE
        CreateTorneoRequest request = new CreateTorneoRequest();
        request.setNombre("Torneo Prueba");
        request.setCategoriaId(999L);
        request.setSedeId(1L);
        request.setJuezResponsableId(2L);
        
        when(categoriaRepository.findById(999L)).thenReturn(Optional.empty());
        
        // ACT & ASSERT
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> torneoService.createTorneo(request)
        );
        
        assertTrue(exception.getMessage().contains("Categoría no encontrada"));
        verify(torneoRepository, never()).save(any(Torneo.class));
    }
    
    @Test
    public void testCrearTorneo_DebeRechazarSedeInexistente() {
        // ARRANGE
        CreateTorneoRequest request = new CreateTorneoRequest();
        request.setNombre("Torneo Prueba");
        request.setCategoriaId(1L);
        request.setSedeId(999L);
        request.setJuezResponsableId(2L);
        
        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setActiva(true);
        
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(sedeRepository.findById(999L)).thenReturn(Optional.empty());
        
        // ACT & ASSERT
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> torneoService.createTorneo(request)
        );
        
        assertTrue(exception.getMessage().contains("Sede no encontrada"));
        verify(torneoRepository, never()).save(any(Torneo.class));
    }
    
    @Test
    public void testCrearTorneo_DebeRechazarJuezInvalido() {
        // ARRANGE
        CreateTorneoRequest request = new CreateTorneoRequest();
        request.setNombre("Torneo Prueba");
        request.setCategoriaId(1L);
        request.setSedeId(1L);
        request.setJuezResponsableId(999L);
        
        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setActiva(true);
        
        Sede sede = new Sede();
        sede.setId(1L);
        sede.setActiva(true);
        
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(sedeRepository.findById(1L)).thenReturn(Optional.of(sede));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        
        // ACT & ASSERT
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> torneoService.createTorneo(request)
        );
        
        assertTrue(exception.getMessage().contains("Juez no encontrado"));
        verify(torneoRepository, never()).save(any(Torneo.class));
    }
}