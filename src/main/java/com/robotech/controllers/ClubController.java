package com.robotech.controllers;

import com.robotech.dto.ClubDTO;
import com.robotech.models.Club;
import com.robotech.models.User;
import com.robotech.services.ClubService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
@Tag(name = "Clubs", description = "Gestión de clubs de robótica")
@SecurityRequirement(name = "bearerAuth")
public class ClubController {

    private final ClubService clubService;
    private final com.robotech.repositories.UserRepository userRepository;

    @Operation(summary = "Obtener todos los clubs", description = "Retorna la lista de todos los clubs disponibles")
    @GetMapping
    public List<ClubDTO> getAllClubs() {
        List<Club> clubs = clubService.getAllClubs();
        return clubs.stream().map(this::convertToClubDTO).collect(Collectors.toList());
    }

    /**
     * ✅ MODIFICADO: Solo ADMIN puede crear clubs
     */
    @Operation(summary = "Crear club (SOLO ADMIN)", description = "Crea un nuevo club y asigna un owner")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createClub(@RequestBody CreateClubRequest request) {
        try {
            Club createdClub = clubService.createClubByAdmin(request);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Club creado exitosamente",
                "club", convertToClubDTO(createdClub)
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @Operation(summary = "Obtener mi club", description = "Obtiene el club del usuario autenticado (CLUB_OWNER)")
    @GetMapping("/my-club")
    @PreAuthorize("hasRole('CLUB_OWNER')")
    public ResponseEntity<ClubDTO> getMyClub() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        Club club = clubService.getClubByOwner(user.getId());
        return ResponseEntity.ok(convertToClubDTO(club));
    }

    @Operation(summary = "Obtener miembros de mi club", description = "Lista todos los miembros del club (CLUB_OWNER)")
    @GetMapping("/my-club/miembros")
    @PreAuthorize("hasRole('CLUB_OWNER')")
    public ResponseEntity<List<UserMiembroDTO>> getMiembrosDeMiClub() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        boolean esClubOwner = user.getRoles().stream()
                .anyMatch(role -> "ROLE_CLUB_OWNER".equals(role.getNombre()));
        
        if (!esClubOwner) {
            throw new RuntimeException("No tienes permisos de club owner");
        }
        
        List<User> miembros = clubService.getMiembrosDelClub(user.getId());
        
        return ResponseEntity.ok(
            miembros.stream()
                .map(this::convertToUserMiembroDTO)
                .collect(Collectors.toList())
        );
    }

    /**
     * ✅ CLUB_OWNER puede actualizar su club
     */
    @Operation(summary = "Actualizar mi club", description = "Actualiza los datos del club propio (CLUB_OWNER)")
    @PutMapping("/my-club")
    @PreAuthorize("hasRole('CLUB_OWNER')")
    public ResponseEntity<ClubDTO> updateMyClub(@RequestBody UpdateClubRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        Club updatedClub = clubService.updateMyClub(user.getId(), request);
        return ResponseEntity.ok(convertToClubDTO(updatedClub));
    }

    @Operation(summary = "Endpoint de prueba", description = "Endpoint simple para probar la conexión")
    @GetMapping("/test")
    public String test() {
        return "Club endpoint funcionando - " + java.time.LocalDateTime.now();
    }

private ClubDTO convertToClubDTO(Club club) {
    ClubDTO dto = new ClubDTO();
    dto.setId(club.getId());
    dto.setNombre(club.getNombre());
    dto.setDescripcion(club.getDescripcion());
    dto.setCiudad(club.getCiudad());
    dto.setPais(club.getPais());
    dto.setLogo(club.getLogo());
    
    if (club.getOwner() != null) {
        dto.setOwnerId(club.getOwner().getId());
        dto.setOwnerNombre(club.getOwner().getNombre() + " " + club.getOwner().getApellido());
    }
    
    int cantidadMiembros = club.getMiembros() != null ? club.getMiembros().size() : 0;
    dto.setCantidadMiembros(cantidadMiembros);
    
    // ✅ AGREGAR ESTOS CAMPOS
    int maxParticipantes = club.getMaxParticipantes() != null ? club.getMaxParticipantes() : 16;
    dto.setMaxParticipantes(maxParticipantes);
    dto.setCuposDisponibles(maxParticipantes - cantidadMiembros);
    dto.setActiva(club.getActiva());
    
    return dto;
}

    private UserMiembroDTO convertToUserMiembroDTO(User user) {
        UserMiembroDTO dto = new UserMiembroDTO();
        dto.setId(user.getId());
        dto.setNombre(user.getNombre());
        dto.setApellido(user.getApellido());
        dto.setEmail(user.getEmail());
        dto.setDni(user.getDni());
        dto.setTelefono(user.getTelefono());
        dto.setEstado(user.getEstado());
        dto.setFechaNacimiento(user.getFechaNacimiento());
        dto.setRoles(user.getRoles().stream()
            .map(role -> role.getNombre())
            .collect(Collectors.toList()));
        return dto;
    }

    /**
     * ✅ DTO PARA CREAR CLUB (ADMIN)
     */
    @Data
    public static class CreateClubRequest {
        private String nombre;
        private String descripcion;
        private String ciudad;
        private String pais;
        private String logo;
        private Long ownerId; // ID del usuario que será el owner
        public void setMaxParticipantes(int i) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'setMaxParticipantes'");
        }
    }

    @Data
    public static class UpdateClubRequest {
        private String nombre;
        private String descripcion;
        private String ciudad;
        private String pais;
        private String logo;
    }

    @Data
    public static class UserMiembroDTO {
        private Long id;
        private String nombre;
        private String apellido;
        private String email;
        private String dni;
        private String telefono;
        private String estado;
        private java.time.LocalDate fechaNacimiento;
        private List<String> roles;
    }
}