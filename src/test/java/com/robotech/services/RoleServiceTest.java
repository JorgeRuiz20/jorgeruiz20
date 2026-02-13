package com.robotech.services;

import com.robotech.models.Role;
import com.robotech.models.User;
import com.robotech.repositories.RoleRepository;
import com.robotech.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoleServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleService roleService;

    // ============ DATOS ============
    private User usuario;
    private Role roleUser;
    private Role roleCompetitor;
    private Role roleAdmin;

    @BeforeEach
    void setUp() {
        roleUser = new Role();
        roleUser.setId(1L);
        roleUser.setNombre("ROLE_USER");

        roleCompetitor = new Role();
        roleCompetitor.setId(2L);
        roleCompetitor.setNombre("ROLE_COMPETITOR");

        roleAdmin = new Role();
        roleAdmin.setId(3L);
        roleAdmin.setNombre("ROLE_ADMIN");

        usuario = new User();
        usuario.setId(1L);
        usuario.setNombre("Carlos");
        usuario.setApellido("Mendoza");
        usuario.setEmail("carlos@gmail.com");
        usuario.setRoles(new HashSet<>(Arrays.asList(roleUser, roleCompetitor)));
    }

    // ============ TESTS ============

    @Test
    void testAsignarRoles() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(roleRepository.findByNombre("ROLE_ADMIN")).thenReturn(Optional.of(roleAdmin));
        when(roleRepository.findByNombre("ROLE_USER")).thenReturn(Optional.of(roleUser));
        when(userRepository.save(any(User.class))).thenReturn(usuario);

        User resultado = roleService.asignarRolesUsuario(1L, Arrays.asList("ROLE_ADMIN", "ROLE_USER"));

        assertNotNull(resultado);
        verify(userRepository, times(1)).save(usuario);

        System.out.println("✅ TEST PASADO: Asignar roles a usuario");
    }

    @Test
    void testAsignarRoles_RechazarUsuarioNoExistente() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> roleService.asignarRolesUsuario(999L, Arrays.asList("ROLE_USER"))
        );

        assertTrue(exception.getMessage().contains("Usuario no encontrado"));
        verify(userRepository, never()).save(any());

        System.out.println("✅ TEST PASADO: Rechazar usuario no existente");
    }

    @Test
    void testAsignarRoles_RechazarRolNoExistente() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(roleRepository.findByNombre("ROLE_INEXISTENTE")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> roleService.asignarRolesUsuario(1L, Arrays.asList("ROLE_INEXISTENTE"))
        );

        assertTrue(exception.getMessage().contains("Rol no encontrado"));
        verify(userRepository, never()).save(any());

        System.out.println("✅ TEST PASADO: Rechazar rol no existente");
    }

    @Test
    void testAgregarRol() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(roleRepository.findByNombre("ROLE_ADMIN")).thenReturn(Optional.of(roleAdmin));
        when(userRepository.save(any(User.class))).thenReturn(usuario);

        User resultado = roleService.agregarRolUsuario(1L, "ROLE_ADMIN");

        assertNotNull(resultado);
        verify(userRepository, times(1)).save(usuario);

        System.out.println("✅ TEST PASADO: Agregar rol nuevo");
    }

    @Test
    void testRemoverRol() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(roleRepository.findByNombre("ROLE_COMPETITOR")).thenReturn(Optional.of(roleCompetitor));
        when(userRepository.save(any(User.class))).thenReturn(usuario);

        User resultado = roleService.removerRolUsuario(1L, "ROLE_COMPETITOR");

        assertNotNull(resultado);
        verify(userRepository, times(1)).save(usuario);

        System.out.println("✅ TEST PASADO: Remover rol");
    }
}