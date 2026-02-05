package com.robotech.services;

import com.robotech.models.Club;
import com.robotech.models.CodigoRegistro;
import com.robotech.models.User;
import com.robotech.repositories.ClubRepository;
import com.robotech.repositories.CodigoRegistroRepository;
import com.robotech.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CodigoRegistroService {

    private final CodigoRegistroRepository codigoRegistroRepository;
    private final UserRepository userRepository;
    private final ClubRepository clubRepository;

    @Transactional
    public CodigoRegistro generarCodigo(Long generadorId) {
        User generador = userRepository.findById(generadorId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Verificar si es ADMIN o CLUB_OWNER
        boolean isAdmin = generador.getRoles().stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getNombre()));
        
        boolean isClubOwner = generador.getRoles().stream()
                .anyMatch(role -> "ROLE_CLUB_OWNER".equals(role.getNombre()));

        if (!isAdmin && !isClubOwner) {
            throw new RuntimeException("Solo admins y club owners pueden generar códigos");
        }

        Club club;
        
        if (isClubOwner) {
            // Club owner: buscar su club
            club = clubRepository.findByOwnerId(generadorId)
                    .orElseThrow(() -> new RuntimeException("No tienes un club asignado"));
            
            // Validar que el club no esté lleno
            if (club.isFull()) {
                throw new RuntimeException("El club ha alcanzado el máximo de " + 
                    club.getMaxParticipantes() + " participantes. No se pueden generar más códigos");
            }
        } else {
            // Admin: puede generar para cualquier club (por ahora se genera sin club específico)
            // O podrías hacer que el admin especifique el clubId
            throw new RuntimeException("El admin debe especificar un clubId al generar código");
        }

        String codigo = generarCodigoUnico();
        
        CodigoRegistro codigoRegistro = new CodigoRegistro(codigo, generador, club);
        return codigoRegistroRepository.save(codigoRegistro);
    }

    public List<CodigoRegistro> listarTodosCodigos() {
        return codigoRegistroRepository.findAll();
    }

    public List<CodigoRegistro> listarCodigosDisponibles() {
        return codigoRegistroRepository.findByUsadoFalse();
    }

    public List<CodigoRegistro> listarCodigosPorGenerador(Long generadorId) {
        return codigoRegistroRepository.findByGeneradoPorId(generadorId);
    }

    public Optional<CodigoRegistro> buscarPorCodigo(String codigo) {
        return codigoRegistroRepository.findByCodigo(codigo);
    }

    public boolean verificarCodigoValido(String codigo) {
        Optional<CodigoRegistro> codigoOpt = codigoRegistroRepository.findByCodigo(codigo);
        if (codigoOpt.isEmpty() || codigoOpt.get().getUsado()) {
            return false;
        }
        
        // Validar que el club no esté lleno
        Club club = codigoOpt.get().getClub();
        if (club != null && club.isFull()) {
            return false;
        }
        
        return true;
    }

    @Transactional
    public void eliminarCodigoNoUsado(Long codigoId, Long usuarioId) {
        CodigoRegistro codigo = codigoRegistroRepository.findById(codigoId)
                .orElseThrow(() -> new RuntimeException("Código no encontrado"));

        // Validar que el usuario es quien generó el código o es admin
        User usuario = userRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        boolean isAdmin = usuario.getRoles().stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getNombre()));
        
        if (!isAdmin && !codigo.getGeneradoPor().getId().equals(usuarioId)) {
            throw new RuntimeException("No tienes permisos para eliminar este código");
        }

        if (codigo.getUsado()) {
            throw new RuntimeException("No se puede eliminar un código que ya ha sido usado");
        }

        codigoRegistroRepository.delete(codigo);
    }

    private String generarCodigoUnico() {
        String codigo;
        do {
            codigo = "REG-" + generateRandomString(8);
        } while (codigoRegistroRepository.existsByCodigo(codigo));
        return codigo;
    }

    private String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }
}