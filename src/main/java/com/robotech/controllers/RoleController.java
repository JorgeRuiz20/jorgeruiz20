package com.robotech.controllers;

import com.robotech.models.Role;
import com.robotech.models.User;
import com.robotech.services.RoleService;
import com.robotech.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
@Tag(name = "Gestión de Roles", description = "Administración de roles de usuarios (solo ADMIN)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

    private final RoleService roleService;
    private final UserService userService;

    @Operation(summary = "Obtener todos los roles disponibles")
    @GetMapping
    public List<Role> getAllRoles() {
        return roleService.getAllRoles();
    }

    @Operation(summary = "Asignar roles a usuario")
    @PutMapping("/usuario/{userId}")
    public ResponseEntity<User> asignarRolesUsuario(
            @PathVariable Long userId,
            @RequestBody AsignarRolesRequest request) {
        
        User user = roleService.asignarRolesUsuario(userId, request.getRoles());
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Agregar rol a usuario")
    @PostMapping("/usuario/{userId}/rol/{rolNombre}")
    public ResponseEntity<User> agregarRolUsuario(
            @PathVariable Long userId,
            @PathVariable String rolNombre) {
        
        User user = roleService.agregarRolUsuario(userId, rolNombre);
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Remover rol de usuario")
    @DeleteMapping("/usuario/{userId}/rol/{rolNombre}")
    public ResponseEntity<User> removerRolUsuario(
            @PathVariable Long userId,
            @PathVariable String rolNombre) {
        
        User user = roleService.removerRolUsuario(userId, rolNombre);
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Obtener usuarios por rol")
    @GetMapping("/rol/{rolNombre}")
    public List<User> getUsuariosPorRol(@PathVariable String rolNombre) {
        return roleService.getUsuariosPorRol(rolNombre);
    }

    @Operation(summary = "Obtener todos los jueces")
    @GetMapping("/jueces")
    public List<User> getJueces() {
        return roleService.getUsuariosPorRol("ROLE_JUDGE");
    }

    @Operation(summary = "Obtener todos los club owners")
    @GetMapping("/club-owners")
    public List<User> getClubOwners() {
        return roleService.getUsuariosPorRol("ROLE_CLUB_OWNER");
    }

    @Data
    public static class AsignarRolesRequest {
        private List<String> roles;
    }
}