package com.robotech.services;

import com.robotech.dto.CreateUserRequest;
import com.robotech.models.Club;
import com.robotech.models.Role;
import com.robotech.models.User;
import com.robotech.repositories.ClubRepository;
import com.robotech.repositories.RoleRepository;
import com.robotech.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private SimilarityService similarityService;

    @InjectMocks
    private AdminService adminService;

    // ============ DATOS ============
    private CreateUserRequest request;
    private Role roleJuez;
    private User usuarioCreado;

    @BeforeEach
    void setUp() {
        roleJuez = new Role();
        roleJuez.setId(4L);
        roleJuez.setNombre("ROLE_JUDGE");

        request = new CreateUserRequest();
        request.setDni("87654321");
        request.setNombre("María");
        request.setApellido("González");
        request.setEmail("maria@robotech.com");
        request.setTelefono("+51999888777");
        request.setFechaNacimiento(LocalDate.of(1990, 5, 15));
        request.setRoles(Arrays.asList("ROLE_JUDGE"));

        usuarioCreado = new User();
        usuarioCreado.setId(1L);
        usuarioCreado.setDni("87654321");
        usuarioCreado.setNombre("María");
        usuarioCreado.setApellido("González");
        usuarioCreado.setEmail("maria@robotech.com");
        usuarioCreado.setEstado("APROBADO");
    }

    // ============ TESTS ============

    @Test
    void testCrearUsuarioConExito() {
        when(similarityService.validarDNI(anyString())).thenReturn(true);
        when(similarityService.validarNombre(anyString())).thenReturn(true);
        when(similarityService.validarEmail(anyString())).thenReturn(true);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.existsByDni(anyString())).thenReturn(false);
        when(userRepository.findAll()).thenReturn(Collections.emptyList());
        when(similarityService.existeNombreCompletoSimilar(anyString(), anyList())).thenReturn(false);
        when(roleRepository.findByNombre("ROLE_JUDGE")).thenReturn(Optional.of(roleJuez));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(usuarioCreado);
        doNothing().when(emailService).sendCredentialsEmail(anyString(), anyString(), anyString(), anyList());

        Map<String, Object> resultado = adminService.createUserAndSendCredentials(request);

        assertNotNull(resultado);
        assertEquals(true, resultado.get("success"));
        assertNotNull(resultado.get("temporalPassword"));
        verify(userRepository, times(1)).save(any(User.class));
        verify(emailService, times(1)).sendCredentialsEmail(anyString(), anyString(), anyString(), anyList());

        System.out.println("✅ TEST PASADO: Crear usuario con rol");
    }

    @Test
    void testCrearUsuario_RechazarEmailDuplicado() {
        when(similarityService.validarDNI(anyString())).thenReturn(true);
        when(similarityService.validarNombre(anyString())).thenReturn(true);
        when(similarityService.validarEmail(anyString())).thenReturn(true);
        when(userRepository.findByEmail("maria@robotech.com")).thenReturn(Optional.of(usuarioCreado));

        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> adminService.createUserAndSendCredentials(request)
        );

        assertTrue(exception.getMessage().contains("email ya está registrado"));
        verify(userRepository, never()).save(any());

        System.out.println("✅ TEST PASADO: Rechazar email duplicado");
    }

    @Test
    void testCrearUsuario_RechazarDNIDuplicado() {
        when(similarityService.validarDNI(anyString())).thenReturn(true);
        when(similarityService.validarNombre(anyString())).thenReturn(true);
        when(similarityService.validarEmail(anyString())).thenReturn(true);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.existsByDni("87654321")).thenReturn(true);

        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> adminService.createUserAndSendCredentials(request)
        );

        assertTrue(exception.getMessage().contains("DNI ya está registrado"));
        verify(userRepository, never()).save(any());

        System.out.println("✅ TEST PASADO: Rechazar DNI duplicado");
    }

    @Test
    void testCrearUsuario_RechazarRolNoExistente() {
        when(similarityService.validarDNI(anyString())).thenReturn(true);
        when(similarityService.validarNombre(anyString())).thenReturn(true);
        when(similarityService.validarEmail(anyString())).thenReturn(true);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.existsByDni(anyString())).thenReturn(false);
        when(userRepository.findAll()).thenReturn(Collections.emptyList());
        when(similarityService.existeNombreCompletoSimilar(anyString(), anyList())).thenReturn(false);
        when(roleRepository.findByNombre("ROLE_JUDGE")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> adminService.createUserAndSendCredentials(request)
        );

        assertTrue(exception.getMessage().contains("Rol no encontrado"));
        verify(userRepository, never()).save(any());

        System.out.println("✅ TEST PASADO: Rechazar rol no existente");
    }
}