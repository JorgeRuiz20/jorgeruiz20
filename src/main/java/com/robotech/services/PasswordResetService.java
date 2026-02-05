package com.robotech.services;

import com.robotech.models.PasswordResetToken;
import com.robotech.models.User;
import com.robotech.repositories.PasswordResetTokenRepository;
import com.robotech.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void createPasswordResetToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No existe un usuario con ese email"));

        // Eliminar tokens anteriores del usuario
        tokenRepository.deleteByUser(user);

        // Generar nuevo token único
        String token = UUID.randomUUID().toString();

        // Crear y guardar token
        PasswordResetToken resetToken = new PasswordResetToken(token, user);
        tokenRepository.save(resetToken);

        // Enviar email
        try {
            emailService.sendPasswordResetEmail(
                user.getEmail(), 
                token, 
                user.getNombre() + " " + user.getApellido()
            );
        } catch (Exception e) {
            // Si falla el envío del email, eliminar el token
            tokenRepository.delete(resetToken);
            throw new RuntimeException("Error al enviar el email de recuperación: " + e.getMessage());
        }
    }

    @Transactional
    public void resetPassword(String token, String newPassword, String confirmPassword) {
        // Validar que las contraseñas coincidan
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Las contraseñas no coinciden");
        }

        // Buscar token
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token inválido o expirado"));

        // Validar que no esté usado
        if (resetToken.getUsado()) {
            throw new RuntimeException("Este token ya ha sido utilizado");
        }

        // Validar que no esté expirado
        if (resetToken.isExpired()) {
            throw new RuntimeException("El token ha expirado. Solicita uno nuevo");
        }

        // Actualizar contraseña del usuario
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Marcar token como usado
        resetToken.setUsado(true);
        resetToken.setFechaUso(LocalDateTime.now());
        tokenRepository.save(resetToken);
    }

    public boolean validateToken(String token) {
        return tokenRepository.findByToken(token)
                .map(resetToken -> !resetToken.getUsado() && !resetToken.isExpired())
                .orElse(false);
    }

    // Limpieza automática de tokens expirados cada día a las 2 AM
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanExpiredTokens() {
        tokenRepository.deleteAllExpiredTokens(LocalDateTime.now());
    }
}