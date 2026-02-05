package com.robotech.services;

import com.robotech.controllers.ClubController;
import com.robotech.models.Club;
import com.robotech.models.User;
import com.robotech.repositories.ClubRepository;
import com.robotech.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClubService {

    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final SimilarityService similarityService;

    public List<Club> getAllClubs() {
        return clubRepository.findAll();
    }

    /**
     * ✅ NUEVO: Solo ADMIN puede crear clubs
     */
    @Transactional
    public Club createClubByAdmin(ClubController.CreateClubRequest request) {
        // Validar que el owner existe
        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new RuntimeException("Usuario owner no encontrado"));
        
        // Verificar que el usuario tiene rol CLUB_OWNER
        boolean isClubOwner = owner.getRoles().stream()
                .anyMatch(role -> "ROLE_CLUB_OWNER".equals(role.getNombre()));
        
        if (!isClubOwner) {
            throw new RuntimeException("El usuario debe tener el rol CLUB_OWNER");
        }
        
        // Validar que el owner no tenga ya un club
        if (clubRepository.findByOwnerId(request.getOwnerId()).isPresent()) {
            throw new RuntimeException("Este usuario ya tiene un club asignado");
        }
        
        // Validar nombre del club
        String nombreNuevo = request.getNombre();
        if (nombreNuevo == null || nombreNuevo.isBlank()) {
            throw new RuntimeException("El nombre del club no puede estar vacío");
        }
        
        List<String> nombresExistentes = clubRepository.findAll().stream()
                .map(Club::getNombre)
                .collect(Collectors.toList());
        
        if (similarityService.existeClubSimilar(nombreNuevo, nombresExistentes)) {
            String similar = similarityService.encontrarClubSimilar(nombreNuevo, nombresExistentes);
            throw new RuntimeException("Ya existe un club con nombre similar: '" + similar + "'");
        }
        
        Club club = new Club();
        club.setNombre(request.getNombre());
        club.setDescripcion(request.getDescripcion());
        club.setCiudad(request.getCiudad());
        club.setPais(request.getPais());
        club.setLogo(request.getLogo());
        club.setOwner(owner);
        club.setMaxParticipantes(16);
        
        return clubRepository.save(club);
    }

    public Club getClubByOwner(Long ownerId) {
        return clubRepository.findClubByOwner(ownerId)
                .orElseThrow(() -> new RuntimeException("No se encontró club para este owner"));
    }
    
    public Club getClubById(Long clubId) {
        return clubRepository.findById(clubId)
                .orElseThrow(() -> new RuntimeException("Club no encontrado"));
    }
    
    @Transactional
    public Club updateClub(Long clubId, Club clubData, Long ownerId) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new RuntimeException("Club no encontrado"));
        
        if (!club.getOwner().getId().equals(ownerId)) {
            throw new RuntimeException("Solo el dueño del club puede modificarlo");
        }
        
        club.setNombre(clubData.getNombre());
        club.setDescripcion(clubData.getDescripcion());
        club.setCiudad(clubData.getCiudad());
        club.setPais(clubData.getPais());
        club.setLogo(clubData.getLogo());
        
        return clubRepository.save(club);
    }

    @Transactional
    public Club updateMyClub(Long ownerId, ClubController.UpdateClubRequest request) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        boolean isClubOwner = owner.getRoles().stream()
                .anyMatch(role -> "ROLE_CLUB_OWNER".equals(role.getNombre()));
        
        if (!isClubOwner) {
            throw new RuntimeException("Solo los club owners pueden actualizar clubs");
        }
        
        Club club = clubRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new RuntimeException("No tienes un club asignado"));
        
        // Validar similitud si se cambia el nombre
        if (request.getNombre() != null && !request.getNombre().isBlank() && 
            !request.getNombre().equals(club.getNombre())) {
            
            List<String> nombresExistentes = clubRepository.findAll().stream()
                    .filter(c -> !c.getId().equals(club.getId()))
                    .map(Club::getNombre)
                    .collect(Collectors.toList());
            
            if (similarityService.existeClubSimilar(request.getNombre(), nombresExistentes)) {
                String similar = similarityService.encontrarClubSimilar(request.getNombre(), nombresExistentes);
                throw new RuntimeException("Ya existe un club con nombre similar: '" + similar + "'");
            }
            
            club.setNombre(request.getNombre());
        }
        
        if (request.getDescripcion() != null) {
            club.setDescripcion(request.getDescripcion());
        }
        
        if (request.getCiudad() != null) {
            club.setCiudad(request.getCiudad());
        }
        
        if (request.getPais() != null) {
            club.setPais(request.getPais());
        }
        
        if (request.getLogo() != null) {
            club.setLogo(request.getLogo());
        }
        
        return clubRepository.save(club);
    }

    public List<User> getMiembrosDelClub(Long ownerId) {
        Club club = clubRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new RuntimeException("No tienes un club asignado"));
        
        return userRepository.findByClubIdAndEstado(club.getId(), "APROBADO");
    }
}