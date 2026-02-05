package com.robotech.controllers;

import com.robotech.dto.CodigoRegistroDTO;
import com.robotech.models.CodigoRegistro;
import com.robotech.services.CodigoRegistroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/codigos-registro")
@RequiredArgsConstructor
@Tag(name = "Códigos de Registro")
@SecurityRequirement(name = "bearerAuth")
public class CodigoRegistroController {

    private final CodigoRegistroService codigoRegistroService;
    private final com.robotech.repositories.UserRepository userRepository;

    @Operation(summary = "Generar código (ADMIN o CLUB_OWNER)")
    @PostMapping("/generar")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLUB_OWNER')")
    public ResponseEntity<CodigoRegistroDTO> generarCodigo(Authentication authentication) {
        String email = authentication.getName();
        var user = userRepository.findByEmail(email).orElseThrow();
        
        CodigoRegistro codigo = codigoRegistroService.generarCodigo(user.getId());
        return ResponseEntity.ok(convertToDTO(codigo));
    }

    @Operation(summary = "Listar mis códigos (ADMIN o CLUB_OWNER)")
    @GetMapping("/mis-codigos")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLUB_OWNER')")
    public ResponseEntity<List<CodigoRegistroDTO>> listarMisCodigos(Authentication authentication) {
        String email = authentication.getName();
        var user = userRepository.findByEmail(email).orElseThrow();
        
        List<CodigoRegistro> codigos = codigoRegistroService.listarCodigosPorGenerador(user.getId());
        return ResponseEntity.ok(codigos.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }

    @Operation(summary = "Listar todos los códigos (solo ADMIN)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CodigoRegistroDTO>> listarTodosCodigos() {
        List<CodigoRegistro> codigos = codigoRegistroService.listarTodosCodigos();
        return ResponseEntity.ok(codigos.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }

    @GetMapping("/verificar/{codigo}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<VerificarCodigoResponse> verificarCodigo(@PathVariable String codigo) {
        boolean valido = codigoRegistroService.verificarCodigoValido(codigo);
        CodigoRegistro codigoObj = null;
        
        if (valido) {
            codigoObj = codigoRegistroService.buscarPorCodigo(codigo).orElse(null);
        }
        
        VerificarCodigoResponse response = new VerificarCodigoResponse();
        response.setValido(valido);
        response.setMensaje(valido ? "Código válido" : "Código inválido o ya usado");
        
        if (codigoObj != null && codigoObj.getClub() != null) {
            response.setClubNombre(codigoObj.getClub().getNombre());
            response.setClubId(codigoObj.getClub().getId());
            response.setCuposDisponibles(codigoObj.getClub().getCuposDisponibles());
            response.setMaxParticipantes(codigoObj.getClub().getMaxParticipantes());
        }
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{codigoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLUB_OWNER')")
    public ResponseEntity<String> eliminarCodigo(@PathVariable Long codigoId, Authentication authentication) {
        String email = authentication.getName();
        var user = userRepository.findByEmail(email).orElseThrow();
        
        codigoRegistroService.eliminarCodigoNoUsado(codigoId, user.getId());
        return ResponseEntity.ok("Código eliminado correctamente");
    }

    private CodigoRegistroDTO convertToDTO(CodigoRegistro codigo) {
        CodigoRegistroDTO dto = new CodigoRegistroDTO();
        dto.setId(codigo.getId());
        dto.setCodigo(codigo.getCodigo());
        dto.setUsado(codigo.getUsado());
        dto.setFechaGeneracion(codigo.getFechaGeneracion());
        dto.setFechaUso(codigo.getFechaUso());
        
        if (codigo.getGeneradoPor() != null) {
            dto.setGeneradoPorNombre(codigo.getGeneradoPor().getNombre() + " " + 
                                    codigo.getGeneradoPor().getApellido());
        }
        
        if (codigo.getUsadoPor() != null) {
            dto.setUsadoPorNombre(codigo.getUsadoPor().getNombre() + " " + 
                                 codigo.getUsadoPor().getApellido());
            dto.setUsadoPorEmail(codigo.getUsadoPor().getEmail());
        }
        
        if (codigo.getClub() != null) {
            dto.setClubId(codigo.getClub().getId());
            dto.setClubNombre(codigo.getClub().getNombre());
        }
        
        return dto;
    }

    @Data
    public static class VerificarCodigoResponse {
        private Boolean valido;
        private String mensaje;
        private String clubNombre;
        private Long clubId;
        private Integer cuposDisponibles;
        private Integer maxParticipantes;
    }
}