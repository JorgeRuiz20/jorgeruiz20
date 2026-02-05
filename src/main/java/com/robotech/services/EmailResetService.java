package com.robotech.services;

import com.robotech.models.EmailResetToken;
import com.robotech.models.User;
import com.robotech.repositories.EmailResetTokenRepository;
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
public class EmailResetService {

    private final EmailResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SimilarityService similarityService;

    /**
     * ✅ PASO 1: Buscar usuario por DNI
     */
    public User buscarUsuarioPorDNI(String dni) {
        // Validar formato DNI
        if (!similarityService.validarDNI(dni)) {
            throw new RuntimeException("DNI inválido. Debe ser exactamente 8 dígitos numéricos");
        }
        
        return userRepository.findByDni(dni)
                .orElseThrow(() -> new RuntimeException("No existe un usuario con el DNI: " + dni));
    }

    /**
     * ✅ PASO 2: Restablecer email (genera token, nueva contraseña y envía email)
     */
    @Transactional
    public String restablecerEmail(String dni, String nuevoEmail, Long adminId) {
        // Validar DNI
        if (!similarityService.validarDNI(dni)) {
            throw new RuntimeException("DNI inválido. Debe ser exactamente 8 dígitos numéricos");
        }
        
        // Validar email
        if (!similarityService.validarEmail(nuevoEmail)) {
            throw new RuntimeException("El formato del email no es válido");
        }
        
        // Buscar usuario por DNI
        User usuario = userRepository.findByDni(dni)
                .orElseThrow(() -> new RuntimeException("No existe un usuario con el DNI: " + dni));
        
        // Validar que el nuevo email no esté en uso
        if (userRepository.findByEmail(nuevoEmail).isPresent()) {
            throw new RuntimeException("El email " + nuevoEmail + " ya está registrado en otra cuenta");
        }
        
        // Buscar admin
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Administrador no encontrado"));
        
        boolean esAdmin = admin.getRoles().stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getNombre()));
        
        if (!esAdmin) {
            throw new RuntimeException("Solo los administradores pueden restablecer emails");
        }

        // Eliminar tokens anteriores del usuario
        tokenRepository.deleteByUser(usuario);

        // Generar nueva contraseña temporal
        String nuevaPasswordTemporal = generateTemporalPassword();
        
        // Generar token único
        String token = UUID.randomUUID().toString();

        // Crear y guardar token
        EmailResetToken resetToken = new EmailResetToken(
            token, 
            usuario, 
            nuevoEmail, 
            passwordEncoder.encode(nuevaPasswordTemporal),
            admin
        );
        tokenRepository.save(resetToken);

        // ✅ APLICAR CAMBIOS INMEDIATAMENTE (sin esperar confirmación)
        String emailAnterior = usuario.getEmail();
        usuario.setEmail(nuevoEmail);
        usuario.setPassword(passwordEncoder.encode(nuevaPasswordTemporal));
        userRepository.save(usuario);

        // Enviar email con nuevas credenciales
        try {
            emailService.sendEmailResetCredentials(
                nuevoEmail,
                usuario.getNombre() + " " + usuario.getApellido(),
                nuevoEmail,
                nuevaPasswordTemporal,
                emailAnterior
            );
        } catch (Exception e) {
            // Si falla el envío de email, revertir cambios
            usuario.setEmail(emailAnterior);
            userRepository.save(usuario);
            tokenRepository.delete(resetToken);
            throw new RuntimeException("Error al enviar el email: " + e.getMessage());
        }

        // Marcar token como usado
        resetToken.setUsado(true);
        resetToken.setFechaUso(LocalDateTime.now());
        tokenRepository.save(resetToken);

        return nuevaPasswordTemporal;
    }

    /**
     * Generar contraseña temporal segura
     */
    private String generateTemporalPassword() {
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "@!*?";

        StringBuilder password = new StringBuilder();

        for (int i = 0; i < 2; i++) password.append(upper.charAt((int) (Math.random() * upper.length())));
        for (int i = 0; i < 2; i++) password.append(lower.charAt((int) (Math.random() * lower.length())));
        for (int i = 0; i < 2; i++) password.append(digits.charAt((int) (Math.random() * digits.length())));
        for (int i = 0; i < 2; i++) password.append(special.charAt((int) (Math.random() * special.length())));

        // Mezclar caracteres
        char[] arr = password.toString().toCharArray();
        for (int i = arr.length - 1; i > 0; i--) {
            int j = (int) (Math.random() * (i + 1));
            char tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }

        return new String(arr);
    }

    /**
     * Limpieza automática de tokens expirados cada día a las 3 AM
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanExpiredTokens() {
        tokenRepository.deleteAllExpiredTokens(LocalDateTime.now());
    }
}