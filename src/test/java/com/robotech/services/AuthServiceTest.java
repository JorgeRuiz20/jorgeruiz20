package com.robotech.services;

import com.robotech.dto.RegisterRequest;
import com.robotech.models.*;
import com.robotech.repositories.*;
import com.robotech.security.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ===============================================================
 * PRUEBA 1: REGISTRO DE USUARIO - AuthServiceTest
 * ===============================================================
 * 
 * Prueba el flujo completo de registro de usuarios con código de club.
 */
@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private RoleRepository roleRepository;
    
    @Mock
    private ClubRepository clubRepository;
    
    @Mock
    private CodigoRegistroRepository codigoRegistroRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private AuthenticationManager authenticationManager;
    
    @Mock
    private com.robotech.security.JwtService jwtService;
    
    @Mock
    private SimilarityService similarityService;
    
    @InjectMocks
    private AuthService authService;
    
    @Test
    public void testRegistroUsuario_DebeFuncionarConDatosValidos() {
        // =============== ARRANGE ===============
        
        // 1. Preparar el request de registro
        RegisterRequest request = new RegisterRequest();
        request.setDni("12345678");
        request.setNombre("Carlos");
        request.setApellido("Mendoza");
        request.setEmail("carlos.mendoza@gmail.com");
        request.setPassword("Carlos123@"); // Patrón correcto con @
        request.setTelefono("+51987654321");
        request.setFechaNacimiento(LocalDate.of(2005, 5, 15));
        request.setCodigoRegistro("CLUB-2024-001");
        request.setFotoPerfil("foto.jpg");
        
        // 2. Mock del código de registro válido
        Club club = new Club();
        club.setId(1L);
        club.setNombre("RoboTech Lima");
        club.setMaxParticipantes(16);
        // Los miembros se inicializan automáticamente como ArrayList vacío
        
        CodigoRegistro codigo = new CodigoRegistro();
        codigo.setId(1L);
        codigo.setCodigo("CLUB-2024-001");
        codigo.setClub(club);
        codigo.setUsado(false);
        
        when(codigoRegistroRepository.findByCodigo("CLUB-2024-001"))
            .thenReturn(Optional.of(codigo));
        
        // 3. Mock de roles
        Role roleUser = new Role();
        roleUser.setId(1L);
        roleUser.setNombre("ROLE_USER");
        
        Role roleCompetitor = new Role();
        roleCompetitor.setId(2L);
        roleCompetitor.setNombre("ROLE_COMPETITOR");
        
        when(roleRepository.findByNombre("ROLE_USER"))
            .thenReturn(Optional.of(roleUser));
        when(roleRepository.findByNombre("ROLE_COMPETITOR"))
            .thenReturn(Optional.of(roleCompetitor));
        
        // 4. Mock de validaciones de SimilarityService
        when(similarityService.validarDNI(anyString())).thenReturn(true);
        when(similarityService.validarNombre(anyString())).thenReturn(true);
        when(similarityService.validarEmail(anyString())).thenReturn(true);
        when(similarityService.existeEmailSimilar(anyString(), anyList())).thenReturn(false);
        when(similarityService.existeNombreCompletoSimilar(anyString(), anyList())).thenReturn(false);
        
        // 5. Mock de validaciones de duplicados
        when(userRepository.existsByDni("12345678")).thenReturn(false);
        when(userRepository.findAll()).thenReturn(Collections.emptyList());
        
        // 6. Mock del password encoder
        when(passwordEncoder.encode("Carlos123@"))
            .thenReturn("$2a$10$encodedPasswordHash");
        
        // 7. Mock del guardado del usuario
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setDni("12345678");
        savedUser.setNombre("Carlos");
        savedUser.setApellido("Mendoza");
        savedUser.setEmail("carlos.mendoza@gmail.com");
        savedUser.setClub(club);
        
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        
        // =============== ACT ===============
        authService.register(request);
        
        // =============== ASSERT ===============
        
        // Verificar que se guardó el usuario con los datos correctos
        verify(userRepository, times(1)).save(argThat(user -> 
            user.getDni().equals("12345678") &&
            user.getNombre().equals("Carlos") &&
            user.getApellido().equals("Mendoza") &&
            user.getEmail().equals("carlos.mendoza@gmail.com") &&
            user.getClub().getId().equals(1L) &&
            user.getEstado().equals("APROBADO") &&
            user.getRoles().size() == 2
        ));
        
        // Verificar que el código se marcó como usado
        verify(codigoRegistroRepository, times(1)).save(argThat(cod -> 
            cod.getUsado() == true &&
            cod.getUsadoPor() != null
        ));
        
        // Verificar validaciones
        verify(similarityService, times(1)).validarDNI("12345678");
        verify(similarityService, times(1)).validarNombre("Carlos");
        verify(similarityService, times(1)).validarNombre("Mendoza");
        verify(similarityService, times(1)).validarEmail("carlos.mendoza@gmail.com");
        verify(passwordEncoder, times(1)).encode("Carlos123@");
    }
    
    @Test
    public void testRegistroUsuario_DebeRechazarCodigoYaUsado() {
        // ARRANGE
        RegisterRequest request = new RegisterRequest();
        request.setDni("12345678");
        request.setNombre("Carlos");
        request.setApellido("Mendoza");
        request.setEmail("carlos@gmail.com");
        request.setPassword("Carlos123@");
        request.setCodigoRegistro("CLUB-2024-001");
        
        CodigoRegistro codigoUsado = new CodigoRegistro();
        codigoUsado.setCodigo("CLUB-2024-001");
        codigoUsado.setUsado(true);
        
        when(codigoRegistroRepository.findByCodigo("CLUB-2024-001"))
            .thenReturn(Optional.of(codigoUsado));
        
        when(similarityService.validarDNI(anyString())).thenReturn(true);
        when(similarityService.validarNombre(anyString())).thenReturn(true);
        when(similarityService.validarEmail(anyString())).thenReturn(true);
        
        // ACT & ASSERT
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> authService.register(request)
        );
        
        assertTrue(exception.getMessage().contains("ya ha sido utilizado"));
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    public void testRegistroUsuario_DebeRechazarDNIDuplicado() {
        // ARRANGE
        RegisterRequest request = new RegisterRequest();
        request.setDni("12345678");
        request.setNombre("Carlos");
        request.setApellido("Mendoza");
        request.setEmail("carlos@gmail.com");
        request.setPassword("Carlos123@");
        request.setTelefono("+51987654321");
        request.setFechaNacimiento(LocalDate.of(2005, 5, 15));
        request.setCodigoRegistro("CLUB-2024-001");
        
        CodigoRegistro codigo = new CodigoRegistro();
        codigo.setCodigo("CLUB-2024-001");
        codigo.setUsado(false);
        Club club = new Club();
        codigo.setClub(club);
        
        when(codigoRegistroRepository.findByCodigo("CLUB-2024-001"))
            .thenReturn(Optional.of(codigo));
        
        when(similarityService.validarDNI(anyString())).thenReturn(true);
        when(similarityService.validarNombre(anyString())).thenReturn(true);
        when(similarityService.validarEmail(anyString())).thenReturn(true);
        when(similarityService.existeEmailSimilar(anyString(), anyList())).thenReturn(false);
        when(similarityService.existeNombreCompletoSimilar(anyString(), anyList())).thenReturn(false);
        when(userRepository.findAll()).thenReturn(Collections.emptyList());
        when(userRepository.existsByDni("12345678")).thenReturn(true);
        
        // ACT & ASSERT
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> authService.register(request)
        );
        
        assertTrue(exception.getMessage().contains("DNI ya está registrado"));
        verify(userRepository, never()).save(any(User.class));
    }
}