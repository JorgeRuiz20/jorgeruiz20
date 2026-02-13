package com.robotech.services;

import com.robotech.models.Club;
import com.robotech.models.CodigoRegistro;
import com.robotech.models.Role;
import com.robotech.models.User;
import com.robotech.repositories.ClubRepository;
import com.robotech.repositories.CodigoRegistroRepository;
import com.robotech.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CodigoRegistroServiceTest {

    @Mock
    private CodigoRegistroRepository codigoRegistroRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClubRepository clubRepository;

    @InjectMocks
    private CodigoRegistroService codigoRegistroService;

    // ============ DATOS ============
    private User clubOwner;
    private Club club;
    private Role roleClubOwner;
    private CodigoRegistro codigo;

    @BeforeEach
    void setUp() {
        club = new Club();
        club.setId(1L);
        club.setNombre("RoboTech Lima");
        club.setMaxParticipantes(20);

        roleClubOwner = new Role();
        roleClubOwner.setId(3L);
        roleClubOwner.setNombre("ROLE_CLUB_OWNER");

        clubOwner = new User();
        clubOwner.setId(1L);
        clubOwner.setNombre("Juan");
        clubOwner.setApellido("Pérez");
        clubOwner.setEmail("juan@club.com");
        clubOwner.setRoles(Set.of(roleClubOwner));

        codigo = new CodigoRegistro();
        codigo.setId(1L);
        codigo.setCodigo("REG-ABC123");
        codigo.setClub(club);
        codigo.setUsado(false);
        codigo.setGeneradoPor(clubOwner);
    }

    // ============ TESTS ============

    @Test
    void testGenerarCodigo_ExitoConClubOwner() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(clubOwner));
        when(clubRepository.findByOwnerId(1L)).thenReturn(Optional.of(club));
        when(codigoRegistroRepository.save(any(CodigoRegistro.class))).thenReturn(codigo);

        CodigoRegistro resultado = codigoRegistroService.generarCodigo(1L);

        assertNotNull(resultado);
        assertEquals("REG-ABC123", resultado.getCodigo());
        assertFalse(resultado.getUsado());
        verify(codigoRegistroRepository, times(1)).save(any(CodigoRegistro.class));

        System.out.println("✅ TEST PASADO: Generar código con club owner");
    }

    @Test
    void testGenerarCodigo_RechazarUsuarioNoExistente() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> codigoRegistroService.generarCodigo(999L)
        );

        assertTrue(exception.getMessage().contains("Usuario no encontrado"));
        verify(codigoRegistroRepository, never()).save(any());

        System.out.println("✅ TEST PASADO: Rechazar usuario no existente");
    }

    @Test
    void testGenerarCodigo_RechazarSinRolClubOwner() {
        Role roleUser = new Role();
        roleUser.setNombre("ROLE_USER");
        clubOwner.setRoles(Set.of(roleUser));

        when(userRepository.findById(1L)).thenReturn(Optional.of(clubOwner));

        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> codigoRegistroService.generarCodigo(1L)
        );

        assertTrue(exception.getMessage().contains("admins y club owners"));
        verify(codigoRegistroRepository, never()).save(any());

        System.out.println("✅ TEST PASADO: Rechazar sin rol CLUB_OWNER");
    }

    @Test
    void testGenerarCodigo_RechazarSinClubAsignado() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(clubOwner));
        when(clubRepository.findByOwnerId(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> codigoRegistroService.generarCodigo(1L)
        );

        assertTrue(exception.getMessage().contains("No tienes un club asignado"));
        verify(codigoRegistroRepository, never()).save(any());

        System.out.println("✅ TEST PASADO: Rechazar sin club asignado");
    }

    @Test
    void testVerificarCodigoValido() {
        when(codigoRegistroRepository.findByCodigo("REG-ABC123"))
            .thenReturn(Optional.of(codigo));

        boolean resultado = codigoRegistroService.verificarCodigoValido("REG-ABC123");

        assertTrue(resultado);

        System.out.println("✅ TEST PASADO: Verificar código válido");
    }

    @Test
    void testVerificarCodigoInvalido_YaUsado() {
        codigo.setUsado(true);
        when(codigoRegistroRepository.findByCodigo("REG-ABC123"))
            .thenReturn(Optional.of(codigo));

        boolean resultado = codigoRegistroService.verificarCodigoValido("REG-ABC123");

        assertFalse(resultado);

        System.out.println("✅ TEST PASADO: Rechazar código ya usado");
    }

    @Test
    void testBuscarPorCodigo() {
        when(codigoRegistroRepository.findByCodigo("REG-ABC123"))
            .thenReturn(Optional.of(codigo));

        Optional<CodigoRegistro> resultado = codigoRegistroService.buscarPorCodigo("REG-ABC123");

        assertTrue(resultado.isPresent());
        assertEquals("REG-ABC123", resultado.get().getCodigo());

        System.out.println("✅ TEST PASADO: Buscar por código");
    }
}