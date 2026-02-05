package com.robotech.controllers;

import com.robotech.dto.UserDTO;
import com.robotech.models.Role;
import com.robotech.models.User;
import com.robotech.repositories.UserRepository;
import com.robotech.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Gestión de usuarios del sistema")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @Operation(summary = "Obtener todos los usuarios", description = "Retorna la lista de todos los usuarios (solo ADMIN)")
@GetMapping
public ResponseEntity<List<UserDTO>> getAll() {
    try {
        List<User> users = userRepository.findAll();
        List<UserDTO> userDTOs = users.stream()
            .map(user -> {
                UserDTO dto = new UserDTO();
                dto.setId(user.getId());
                dto.setNombre(user.getNombre());
                dto.setApellido(user.getApellido());
                dto.setEmail(user.getEmail());
                dto.setDni(user.getDni());
                dto.setTelefono(user.getTelefono());
                dto.setEstado(user.getEstado());
                dto.setFechaNacimiento(user.getFechaNacimiento());
                
                // ✅ Manejo seguro de roles
                if (user.getRoles() != null) {
                    dto.setRoles(user.getRoles().stream()
                        .map(Role::getNombre)
                        .collect(Collectors.toSet()));
                } else {
                    dto.setRoles(new HashSet<>());
                }
                
                // ✅ Manejo seguro de club
                if (user.getClub() != null) {
                    dto.setClubId(user.getClub().getId());
                    dto.setClubNombre(user.getClub().getNombre());
                } else {
                    dto.setClubId(null);
                    dto.setClubNombre(null);
                }
                
                return dto;
            })
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(userDTOs);
        
    } catch (Exception e) {
        System.err.println("❌ Error obteniendo usuarios: " + e.getMessage());
        e.printStackTrace();
        return ResponseEntity.status(500).body(new ArrayList<>());
    }
}

    @Operation(summary = "Obtener usuario por ID", description = "Retorna un usuario específico por su ID")
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(user -> ResponseEntity.ok(convertToUserDTO(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Obtener mi perfil", description = "Retorna el perfil del usuario autenticado")
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getMyProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(user -> ResponseEntity.ok(convertToUserDTO(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Actualizar roles de usuario", description = "Actualiza los roles de un usuario (solo ADMIN)")
    @PutMapping("/{id}/roles")
    public ResponseEntity<UserDTO> updateUserRoles(@PathVariable Long id, @RequestBody Set<Role> roles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        if (!isAdmin) {
            throw new AccessDeniedException("Solo los administradores pueden cambiar roles");
        }

        User updatedUser = userService.updateUserRoles(id, roles);
        return ResponseEntity.ok(convertToUserDTO(updatedUser));
    }

    @Operation(summary = "Actualizar estado de usuario", description = "Actualiza el estado de un usuario (solo ADMIN)")
    @PutMapping("/{id}/estado")
    public ResponseEntity<UserDTO> updateUserEstado(@PathVariable Long id, @RequestBody String estado) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        if (!isAdmin) {
            throw new AccessDeniedException("Solo los administradores pueden cambiar estados");
        }

        User updatedUser = userService.updateUserEstado(id, estado);
        return ResponseEntity.ok(convertToUserDTO(updatedUser));
    }

private UserDTO convertToUserDTO(User user) {
    UserDTO dto = new UserDTO();
    dto.setId(user.getId());
    dto.setNombre(user.getNombre());
    dto.setApellido(user.getApellido());
    dto.setEmail(user.getEmail());
    dto.setRoles(user.getRoles().stream()
        .map(Role::getNombre)
        .collect(Collectors.toSet()));
    
    // ✅ ASEGURAR ESTO:
    if (user.getClub() != null) {
        dto.setClubId(user.getClub().getId());
        dto.setClubNombre(user.getClub().getNombre());
    } else {
        dto.setClubId(null);
        dto.setClubNombre(null);
    }
    
    return dto;
}
}