package com.robotech.controllers;

import com.robotech.dto.AuthResponse;
import com.robotech.dto.LoginRequest;
import com.robotech.dto.RegisterRequest;
import com.robotech.models.User;
import com.robotech.repositories.UserRepository;
import com.robotech.security.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Autenticación", description = "Endpoints para login y registro")
public class AuthController {

    private final AuthService authService;
    private final com.robotech.services.FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    @Operation(summary = "Login de usuario")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Registro de usuario SIN robot (se agregan después)")
    @PostMapping(value = "/register", consumes = "multipart/form-data")
    public ResponseEntity<String> register(
            @RequestParam("userData") String userDataJson,
            @RequestParam(value = "fotoPerfil", required = false) MultipartFile fotoPerfil) {

        try {
            RegisterRequest request = objectMapper.readValue(userDataJson, RegisterRequest.class);

            if (fotoPerfil != null && !fotoPerfil.isEmpty()) {
                String fileName = fileStorageService.storeFile(fotoPerfil);
                request.setFotoPerfil(fileName);
            }

            authService.register(request);
            return ResponseEntity
                    .ok("Usuario registrado correctamente. Ahora puedes iniciar sesión y registrar tus robots.");

        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Error en el formato de los datos: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * ✅ NUEVO: Obtener datos actualizados del usuario actual (desde la BD, no del
     * token)
     * Útil cuando el backend modifica los roles del usuario (ej: después de aprobar
     * ingreso a un club)
     * y el frontend necesita refrescar sin hacer re-login.
     * 
     * Ruta: GET /api/auth/me
     * Seguridad: permitAll en SecurityConfig (ya que /api/auth/** es permitAll),
     * pero el JWT debe ser válido porque usamos Authentication.
     * Si no hay token, authentication.getName() será "anonymousUser" y no
     * encontrará usuario → retorna 401.
     */
    @Operation(summary = "Obtener datos del usuario actual (desde BD)")
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of("message", "No autenticado"));
            }

            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Retorna los mismos campos que el login response
            return ResponseEntity.ok(Map.of(
                    "token", "", // El token actual sigue siendo válido, no se necesita nuevo
                    "email", user.getEmail(),
                    "nombre", user.getNombre() + " " + user.getApellido(),
                    "estado", user.getEstado(),
                    "clubId", user.getClub() != null ? user.getClub().getId() : "null",
                    "dni", user.getDni(),
                    "roles", user.getRoles().stream()
                            .map(r -> r.getNombre())
                            .collect(Collectors.toSet())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Auth service funcionando - " + java.time.LocalDateTime.now());
    }
}