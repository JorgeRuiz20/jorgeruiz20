package com.robotech.services;

import com.robotech.models.PasswordResetToken;
import com.robotech.models.User;
import com.robotech.repositories.PasswordResetTokenRepository;
import com.robotech.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User usuario;
    private PasswordResetToken token;

    @BeforeEach
    void setUp() {
        usuario = new User();
        usuario.setId(1L);
        usuario.setNombre("Carlos");
        usuario.setApellido("Mendoza");
        usuario.setEmail("carlos@gmail.com");
        usuario.setPassword("$2a$10$oldPasswordHash");
        usuario.setEstado("APROBADO");

        token = new PasswordResetToken();
        token.setId(1L);
        token.setToken("abc123xyz");
        token.setUser(usuario);
        token.setExpiryDate(LocalDateTime.now().plusHours(1));
        token.setUsado(false);
    }

    // =========================
    // CREAR TOKEN
    // =========================

    @Test
    void testCrearTokenRecuperacion() {
        when(userRepository.findByEmail("carlos@gmail.com"))
                .thenReturn(Optional.of(usuario));

        when(tokenRepository.save(any(PasswordResetToken.class)))
                .thenReturn(token);

        doNothing().when(emailService)
                .sendPasswordResetEmail(anyString(), anyString(), anyString());

        passwordResetService.createPasswordResetToken("carlos@gmail.com");

        verify(tokenRepository, times(1)).save(any(PasswordResetToken.class));
        verify(emailService, times(1))
                .sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    @Test
    void testCrearToken_RechazarEmailNoExistente() {
        when(userRepository.findByEmail("noexiste@gmail.com"))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> passwordResetService.createPasswordResetToken("noexiste@gmail.com")
        );

        assertTrue(exception.getMessage().contains("No existe") ||
                   exception.getMessage().contains("no encontrado"));

        verify(tokenRepository, never()).save(any());
    }

    // =========================
    // VALIDAR TOKEN
    // =========================

    @Test
    void testValidarToken_TokenValido() {
        when(tokenRepository.findByToken("abc123xyz"))
                .thenReturn(Optional.of(token));

        boolean resultado = passwordResetService.validateToken("abc123xyz");

        assertTrue(resultado);
    }

    @Test
    void testValidarToken_TokenExpirado() {
        token.setExpiryDate(LocalDateTime.now().minusHours(1));

        when(tokenRepository.findByToken("abc123xyz"))
                .thenReturn(Optional.of(token));

        boolean resultado = passwordResetService.validateToken("abc123xyz");

        assertFalse(resultado);
    }

    @Test
    void testValidarToken_TokenNoExistente() {
        when(tokenRepository.findByToken("tokeninvalido"))
                .thenReturn(Optional.empty());

        boolean resultado = passwordResetService.validateToken("tokeninvalido");

        assertFalse(resultado);
    }

    @Test
    void testValidarToken_TokenYaUsado() {
        token.setUsado(true);

        when(tokenRepository.findByToken("abc123xyz"))
                .thenReturn(Optional.of(token));

        boolean resultado = passwordResetService.validateToken("abc123xyz");

        assertFalse(resultado);
    }

    // =========================
    // RESET PASSWORD
    // =========================

    @Test
    void testRestablecerPassword() {
        when(tokenRepository.findByToken("abc123xyz"))
                .thenReturn(Optional.of(token));

        when(passwordEncoder.encode("NewPassword123@"))
                .thenReturn("$2a$10$newHash");

        when(userRepository.save(any(User.class)))
                .thenReturn(usuario);

        when(tokenRepository.save(any(PasswordResetToken.class)))
                .thenReturn(token);

        passwordResetService.resetPassword(
                "abc123xyz",
                "NewPassword123@",
                "NewPassword123@"
        );

        verify(userRepository, times(1)).save(usuario);
        verify(tokenRepository, times(1)).save(token);
    }

    // 🔥 CORREGIDO: SIN STUB INNECESARIO
    @Test
    void testRestablecerPassword_PasswordsNoCoinciden() {

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> passwordResetService.resetPassword(
                        "abc123xyz",
                        "Pass1",
                        "Pass2"
                )
        );

        assertTrue(exception.getMessage().toLowerCase().contains("no coinciden"));

        verify(userRepository, never()).save(any());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void testRestablecerPassword_TokenExpirado() {
        token.setExpiryDate(LocalDateTime.now().minusHours(1));

        when(tokenRepository.findByToken("abc123xyz"))
                .thenReturn(Optional.of(token));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> passwordResetService.resetPassword(
                        "abc123xyz",
                        "NewPass123@",
                        "NewPass123@"
                )
        );

        assertTrue(exception.getMessage().contains("expirado") ||
                   exception.getMessage().contains("ha expirado"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void testRestablecerPassword_TokenNoExistente() {
        when(tokenRepository.findByToken("tokeninvalido"))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> passwordResetService.resetPassword(
                        "tokeninvalido",
                        "NewPass123@",
                        "NewPass123@"
                )
        );

        assertTrue(exception.getMessage().contains("inválido") ||
                   exception.getMessage().contains("expirado"));

        verify(userRepository, never()).save(any());
    }

    // =========================
    // LIMPIEZA TOKENS
    // =========================

    @Test
    void testLimpiarTokensExpirados() {
        PasswordResetToken tokenAntiguo = new PasswordResetToken();
        tokenAntiguo.setId(2L);
        tokenAntiguo.setToken("tokenviejo");
        tokenAntiguo.setUser(usuario);
        tokenAntiguo.setExpiryDate(LocalDateTime.now().minusDays(2));
        tokenAntiguo.setUsado(false);

        assertTrue(tokenAntiguo.isExpired());
    }
}
