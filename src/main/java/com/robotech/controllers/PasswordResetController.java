package com.robotech.controllers;

import com.robotech.dto.ForgotPasswordRequest;
import com.robotech.dto.PasswordResetResponse;
import com.robotech.dto.ResetPasswordRequest;
import com.robotech.services.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/password")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Recuperación de Contraseña", description = "Endpoints para recuperar contraseña olvidada")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @Operation(
        summary = "Solicitar recuperación de contraseña",
        description = "Envía un email con un enlace para restablecer la contraseña"
    )
    @PostMapping("/forgot")
    public ResponseEntity<PasswordResetResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            passwordResetService.createPasswordResetToken(request.getEmail());
            return ResponseEntity.ok(new PasswordResetResponse(
                "Se ha enviado un email con las instrucciones para restablecer tu contraseña. " +
                "Por favor revisa tu bandeja de entrada.", 
                true
            ));
        } catch (RuntimeException e) {
            // Por seguridad, siempre retornamos éxito aunque el email no exista
            // Esto evita que se pueda verificar qué emails están registrados
            return ResponseEntity.ok(new PasswordResetResponse(
                "Si el email está registrado, recibirás las instrucciones para restablecer tu contraseña.", 
                true
            ));
        }
    }

    @Operation(
        summary = "Validar token de recuperación",
        description = "Verifica si un token es válido y no ha expirado"
    )
    @GetMapping("/validate-token")
    public ResponseEntity<PasswordResetResponse> validateToken(@RequestParam String token) {
        try {
            boolean isValid = passwordResetService.validateToken(token);
            
            if (isValid) {
                return ResponseEntity.ok(new PasswordResetResponse(
                    "Token válido", 
                    true
                ));
            } else {
                return ResponseEntity.badRequest().body(new PasswordResetResponse(
                    "Token inválido o expirado", 
                    false
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new PasswordResetResponse(
                "Error al validar el token", 
                false
            ));
        }
    }

    @Operation(
        summary = "Restablecer contraseña",
        description = "Establece una nueva contraseña usando el token recibido por email"
    )
    @PostMapping("/reset")
    public ResponseEntity<PasswordResetResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            passwordResetService.resetPassword(
                request.getToken(), 
                request.getNewPassword(), 
                request.getConfirmPassword()
            );
            
            return ResponseEntity.ok(new PasswordResetResponse(
                "Tu contraseña ha sido restablecida exitosamente. Ya puedes iniciar sesión con tu nueva contraseña.", 
                true
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new PasswordResetResponse(
                e.getMessage(), 
                false
            ));
        }
    }

    @Operation(summary = "Test de recuperación de contraseña")
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Password reset service funcionando - " + java.time.LocalDateTime.now());
    }
}