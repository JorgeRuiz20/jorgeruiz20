package com.robotech.controllers;

import com.robotech.dto.EmailResetRequest;
import com.robotech.models.User;
import com.robotech.repositories.UserRepository;
import com.robotech.services.EmailResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/email-reset")
@RequiredArgsConstructor
@Tag(name = "Restablecimiento de Email", description = "Endpoints para restablecer email de usuarios (solo ADMIN)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class EmailResetController {

    private final EmailResetService emailResetService;
    private final UserRepository userRepository;

    @Operation(
        summary = "Buscar usuario por DNI",
        description = "Busca un usuario por su DNI para restablecer su email"
    )
    @GetMapping("/buscar/{dni}")
    public ResponseEntity<?> buscarUsuarioPorDNI(@PathVariable String dni) {
        try {
            User usuario = emailResetService.buscarUsuarioPorDNI(dni);
            
            UserSearchDTO dto = new UserSearchDTO();
            dto.setId(usuario.getId());
            dto.setNombre(usuario.getNombre());
            dto.setApellido(usuario.getApellido());
            dto.setDni(usuario.getDni());
            dto.setEmail(usuario.getEmail());
            dto.setEstado(usuario.getEstado());
            dto.setClubNombre(usuario.getClub() != null ? usuario.getClub().getNombre() : "Sin club");
            dto.setRoles(usuario.getRoles().stream()
                .map(role -> role.getNombre().replace("ROLE_", ""))
                .collect(Collectors.toList()));
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "usuario", dto
            ));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @Operation(
        summary = "Restablecer email de usuario",
        description = "Cambia el email de un usuario y genera una nueva contraseña temporal. Se envían las credenciales al nuevo email."
    )
    @PostMapping("/restablecer")
    public ResponseEntity<?> restablecerEmail(
            @Valid @RequestBody EmailResetRequest request,
            Authentication authentication) {
        
        try {
            String adminEmail = authentication.getName();
            User admin = userRepository.findByEmail(adminEmail)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            String nuevaPasswordTemporal = emailResetService.restablecerEmail(
                request.getDni(), 
                request.getNuevoEmail(), 
                admin.getId()
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Email restablecido exitosamente. Las credenciales han sido enviadas a: " + request.getNuevoEmail(),
                "nuevoEmail", request.getNuevoEmail(),
                "temporalPassword", nuevaPasswordTemporal
            ));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Test del servicio de restablecimiento de email")
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("✅ Servicio de restablecimiento de email funcionando - " + 
                                java.time.LocalDateTime.now());
    }

    // DTO interno
    @Data
    public static class UserSearchDTO {
        private Long id;
        private String nombre;
        private String apellido;
        private String dni;
        private String email;
        private String estado;
        private String clubNombre;
        private java.util.List<String> roles;
    }
}