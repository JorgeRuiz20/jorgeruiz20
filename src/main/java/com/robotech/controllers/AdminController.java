package com.robotech.controllers;

import com.robotech.dto.CreateUserRequest;
import com.robotech.services.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Endpoints exclusivos para administradores")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @Operation(
        summary = "Crear usuario con rol y enviar credenciales por email",
        description = "El admin crea un usuario, se genera contraseña temporal y se envía por email"
    )
    @PostMapping("/users/create")
    public ResponseEntity<Map<String, Object>> createUserWithRole(
            @Valid @RequestBody CreateUserRequest request) {
        try {
            Map<String, Object> result = adminService.createUserAndSendCredentials(request);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Test endpoint")
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Admin endpoint funcionando - " + java.time.LocalDateTime.now());
    }
}