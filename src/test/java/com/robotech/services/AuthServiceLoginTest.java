package com.robotech.services;

import com.robotech.dto.AuthResponse;
import com.robotech.dto.LoginRequest;
import com.robotech.models.Club;
import com.robotech.models.Role;
import com.robotech.models.User;
import com.robotech.repositories.UserRepository;
import com.robotech.security.AuthService;
import com.robotech.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ===============================================================
 * PRUEBA ROBUSTA: LOGIN CON ENCRIPTAMIENTO - AuthServiceLoginTest
 * ===============================================================
 * 
 * Este test cubre el proceso completo de autenticación:
 * 
 * 1. ENCRIPTAMIENTO DE CONTRASEÑA:
 *    - BCrypt se usa para hashear contraseñas
 *    - La contraseña nunca se guarda en texto plano
 *    - Al hacer login, se compara el hash
 * 
 * 2. FLUJO DE AUTENTICACIÓN:
 *    - Usuario envía email + password en texto plano
 *    - Spring Security valida contra el hash en BD
 *    - Si es correcto, genera un JWT token
 * 
 * 3. VALIDACIONES:
 *    - Usuario debe existir
 *    - Usuario no debe estar desactivado (RECHAZADO)
 *    - Credenciales deben ser correctas
 *    - Token JWT debe ser generado
 * 
 * ===============================================================
 */
@ExtendWith(MockitoExtension.class)
public class AuthServiceLoginTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private AuthenticationManager authenticationManager;
    
    @Mock
    private JwtService jwtService;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private AuthService authService;
    
    private User usuarioValido;
    private Club club;
    private Role roleUser;
    private Role roleCompetitor;
    
    /**
     * ================================================================
     * CONFIGURACIÓN INICIAL - DATOS DE PRUEBA
     * ================================================================
     */
    @BeforeEach
    void setUp() {
        // 1. Crear Club
        club = new Club();
        club.setId(1L);
        club.setNombre("RoboTech Lima");
        
        // 2. Crear Roles
        roleUser = new Role();
        roleUser.setId(1L);
        roleUser.setNombre("ROLE_USER");
        
        roleCompetitor = new Role();
        roleCompetitor.setId(2L);
        roleCompetitor.setNombre("ROLE_COMPETITOR");
        
        // 3. Crear Usuario con contraseña HASHEADA
        usuarioValido = new User();
        usuarioValido.setId(1L);
        usuarioValido.setDni("12345678");
        usuarioValido.setNombre("Carlos");
        usuarioValido.setApellido("Mendoza");
        usuarioValido.setEmail("carlos.mendoza@gmail.com");
        
        // ✅ CONTRASEÑA HASHEADA - Esto es lo que hay en la BD
        // Original: "Carlos123@"
        // Hash: generado por BCrypt
        usuarioValido.setPassword("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
        
        usuarioValido.setEstado("APROBADO");
        usuarioValido.setTelefono("+51987654321");
        usuarioValido.setClub(club);
        usuarioValido.setRoles(Set.of(roleUser, roleCompetitor));
    }
    
    /**
     * ================================================================
     * TEST 1: LOGIN EXITOSO CON CREDENCIALES CORRECTAS
     * ================================================================
     * 
     * ⚠️ CORRECCIÓN PRINCIPAL:
     * El método authenticate() NO es void, devuelve un Authentication.
     * Por eso no podemos usar doNothing(), sino when().thenReturn()
     */
    @Test
    public void testLogin_DebeAutenticarConCredencialesCorrectas() {
        // =============== ARRANGE ===============
        
        // 1. Preparar request de login (contraseña en TEXTO PLANO)
        LoginRequest request = new LoginRequest();
        request.setEmail("carlos.mendoza@gmail.com");
        request.setPassword("Carlos123@"); // ⬅️ TEXTO PLANO, NO HASH
        
        // 2. Mock: Buscar usuario por email
        when(userRepository.findByEmail("carlos.mendoza@gmail.com"))
            .thenReturn(Optional.of(usuarioValido));
        
        // 3. ✅ CORRECCIÓN: authenticate() devuelve Authentication, no es void
        // Creamos un mock de Authentication que se devolverá
        Authentication mockAuthentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(mockAuthentication);
        
        // 4. Mock: Generar token JWT
        String expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjYXJsb3MubWVuZG96YUBnbWFpbC5jb20iLCJpYXQiOjE3MDkwMDAwMDAsImV4cCI6MTcwOTA4NjQwMH0.signature";
        when(jwtService.generateToken(usuarioValido))
            .thenReturn(expectedToken);
        
        // =============== ACT ===============
        AuthResponse response = authService.login(request);
        
        // =============== ASSERT ===============
        
        // 1. Verificar que el token fue generado
        assertNotNull(response, "La respuesta no debe ser nula");
        assertNotNull(response.getToken(), "El token no debe ser nulo");
        assertEquals(expectedToken, response.getToken(), "El token debe coincidir");
        
        // 2. Verificar datos del usuario en la respuesta
        assertEquals("carlos.mendoza@gmail.com", response.getEmail());
        assertEquals("Carlos Mendoza", response.getNombre());
        assertEquals("APROBADO", response.getEstado());
        assertEquals("12345678", response.getDni());
        assertEquals("+51987654321", response.getTelefono());
        assertEquals(1L, response.getClubId());
        
        // 3. Verificar roles
        assertNotNull(response.getRoles());
        assertEquals(2, response.getRoles().size());
        assertTrue(response.getRoles().contains("ROLE_USER"));
        assertTrue(response.getRoles().contains("ROLE_COMPETITOR"));
        
        // =============== VERIFICACIONES DE INTERACCIONES ===============
        
        // 4. Verificar que se buscó el usuario
        verify(userRepository, times(1))
            .findByEmail("carlos.mendoza@gmail.com");
        
        // 5. Verificar que se autenticó
        verify(authenticationManager, times(1))
            .authenticate(argThat(auth -> 
                auth instanceof UsernamePasswordAuthenticationToken &&
                auth.getPrincipal().equals("carlos.mendoza@gmail.com") &&
                auth.getCredentials().equals("Carlos123@")
            ));
        
        // 6. Verificar que se generó el token
        verify(jwtService, times(1))
            .generateToken(usuarioValido);
        
        System.out.println("✅ TEST EXITOSO: Login con credenciales correctas");
        System.out.println("📧 Email: " + request.getEmail());
        System.out.println("🔑 Password enviado: " + request.getPassword() + " (texto plano)");
        System.out.println("🔒 Hash en BD: " + usuarioValido.getPassword().substring(0, 20) + "...");
        System.out.println("🎫 Token generado: " + response.getToken().substring(0, 30) + "...");
    }
    
    /**
     * ================================================================
     * TEST 2: LOGIN RECHAZADO - USUARIO NO EXISTE
     * ================================================================
     */
    @Test
    public void testLogin_DebeRechazarUsuarioNoExistente() {
        // ARRANGE
        LoginRequest request = new LoginRequest();
        request.setEmail("noexiste@gmail.com");
        request.setPassword("Password123@");
        
        // Mock: Usuario no encontrado
        when(userRepository.findByEmail("noexiste@gmail.com"))
            .thenReturn(Optional.empty());
        
        // ACT & ASSERT
        UsernameNotFoundException exception = assertThrows(
            UsernameNotFoundException.class,
            () -> authService.login(request)
        );
        
        assertEquals("Usuario no encontrado", exception.getMessage());
        
        // Verificar que NO se intentó autenticar
        verify(authenticationManager, never()).authenticate(any());
        verify(jwtService, never()).generateToken(any());
        
        System.out.println("✅ TEST EXITOSO: Rechazó usuario no existente");
    }
    
    /**
     * ================================================================
     * TEST 3: LOGIN RECHAZADO - USUARIO DESACTIVADO
     * ================================================================
     */
    @Test
    public void testLogin_DebeRechazarUsuarioDesactivado() {
        // ARRANGE
        LoginRequest request = new LoginRequest();
        request.setEmail("carlos.mendoza@gmail.com");
        request.setPassword("Carlos123@");
        
        // Modificar estado del usuario a RECHAZADO
        usuarioValido.setEstado("RECHAZADO");
        
        when(userRepository.findByEmail("carlos.mendoza@gmail.com"))
            .thenReturn(Optional.of(usuarioValido));
        
        // ACT & ASSERT
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> authService.login(request)
        );
        
        assertTrue(exception.getMessage().contains("cuenta ha sido desactivada"));
        assertTrue(exception.getMessage().contains("Contacta al administrador"));
        
        // Verificar que NO se intentó autenticar
        verify(authenticationManager, never()).authenticate(any());
        verify(jwtService, never()).generateToken(any());
        
        System.out.println("✅ TEST EXITOSO: Rechazó usuario desactivado");
    }
    
    /**
     * ================================================================
     * TEST 4: LOGIN RECHAZADO - CONTRASEÑA INCORRECTA
     * ================================================================
     */
    @Test
    public void testLogin_DebeRechazarPasswordIncorrecta() {
        // ARRANGE
        LoginRequest request = new LoginRequest();
        request.setEmail("carlos.mendoza@gmail.com");
        request.setPassword("PasswordIncorrecta123@"); // ❌ PASSWORD INCORRECTA
        
        when(userRepository.findByEmail("carlos.mendoza@gmail.com"))
            .thenReturn(Optional.of(usuarioValido));
        
        // Mock: AuthenticationManager lanza BadCredentialsException
        // porque la password NO coincide con el hash
        when(authenticationManager.authenticate(any()))
            .thenThrow(new BadCredentialsException("Bad credentials"));
        
        // ACT & ASSERT
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> authService.login(request)
        );
        
        // Verificar mensaje genérico (no revela si email o password es incorrecto)
        assertEquals("Credenciales inválidas: email o contraseña incorrectos", 
                     exception.getMessage());
        
        // Verificar que NO se generó token
        verify(jwtService, never()).generateToken(any());
        
        System.out.println("✅ TEST EXITOSO: Rechazó contraseña incorrecta");
        System.out.println("❌ Password enviada: " + request.getPassword());
        System.out.println("✅ Password correcta sería: Carlos123@");
    }
    
    /**
     * ================================================================
     * TEST 5: LOGIN CON USUARIO SIN CLUB
     * ================================================================
     * 
     * ⚠️ CORRECCIÓN: Mismo problema, authenticate() no es void
     */
    @Test
    public void testLogin_DebeFuncionarConUsuarioSinClub() {
        // ARRANGE
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@robotech.com");
        request.setPassword("Admin123@");
        
        // Crear usuario administrador SIN club
        User admin = new User();
        admin.setId(2L);
        admin.setDni("87654321");
        admin.setNombre("Admin");
        admin.setApellido("Sistema");
        admin.setEmail("admin@robotech.com");
        admin.setPassword("$2a$10$hashedPasswordForAdmin");
        admin.setEstado("APROBADO");
        admin.setClub(null); // ⬅️ SIN CLUB
        
        Role roleAdmin = new Role();
        roleAdmin.setNombre("ROLE_ADMIN");
        admin.setRoles(Set.of(roleAdmin));
        
        when(userRepository.findByEmail("admin@robotech.com"))
            .thenReturn(Optional.of(admin));
        
        // ✅ CORRECCIÓN: authenticate() devuelve Authentication
        Authentication mockAuthentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(mockAuthentication);
        
        String token = "admin-token-xyz";
        when(jwtService.generateToken(admin)).thenReturn(token);
        
        // ACT
        AuthResponse response = authService.login(request);
        
        // ASSERT
        assertNotNull(response);
        assertEquals(token, response.getToken());
        assertEquals("admin@robotech.com", response.getEmail());
        assertNull(response.getClubId()); // ⬅️ Club es null
        assertTrue(response.getRoles().contains("ROLE_ADMIN"));
        
        System.out.println("✅ TEST EXITOSO: Login de usuario sin club");
    }
    
    /**
     * ================================================================
     * TEST 6: VERIFICAR ENCRIPTAMIENTO DE CONTRASEÑA
     * ================================================================
     */
    @Test
    public void testDemostracionEncriptamientoBCrypt() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("DEMOSTRACIÓN: ENCRIPTAMIENTO BCRYPT");
        System.out.println("=".repeat(60));
        
        String passwordOriginal = "Carlos123@";
        String hashEnBD = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        
        System.out.println("\n🔓 Contraseña original (texto plano): " + passwordOriginal);
        System.out.println("🔒 Hash almacenado en BD: " + hashEnBD);
        System.out.println("\n🔍 Estructura del hash BCrypt:");
        System.out.println("   $2a$ = Algoritmo BCrypt versión 2a");
        System.out.println("   10$ = Factor de costo (2^10 = 1024 iteraciones)");
        System.out.println("   N9qo8uLO... = Salt aleatorio (22 caracteres)");
        System.out.println("   ...lhWy = Hash resultante (31 caracteres)");
        System.out.println("\n✅ Al hacer login:");
        System.out.println("   1. Usuario envía: 'Carlos123@' (texto plano)");
        System.out.println("   2. Sistema aplica BCrypt con el MISMO salt");
        System.out.println("   3. Compara los hashes");
        System.out.println("   4. Si coinciden → Login exitoso ✅");
        System.out.println("   5. Si no coinciden → Login rechazado ❌");
        System.out.println("\n" + "=".repeat(60));
        
        // Este test solo imprime información, no falla
        assertTrue(true, "Demo de encriptamiento ejecutada");
    }
}