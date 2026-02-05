package com.robotech.services;

import com.robotech.models.Sede;
import com.robotech.repositories.SedeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SedeService {
    
    private final SedeRepository sedeRepository;
    private final SimilarityService similarityService;

    public List<Sede> getSedesActivas() {
        return sedeRepository.findByActivaTrue();
    }

    public Sede getSedeById(Long id) {
        return sedeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));
    }

    public List<Sede> getSedesByDistrito(String distrito) {
        return sedeRepository.findByDistrito(distrito);
    }

    @Transactional
    public Sede crearSede(Sede sede) {
        // Validar campos obligatorios
        if (sede.getNombre() == null || sede.getNombre().isBlank()) {
            throw new RuntimeException("El nombre de la sede es obligatorio");
        }

        // ✅ Validación con Jaro-Winkler (umbral 0.85 para sedes - más estricto)
        List<String> nombresExistentes = sedeRepository.findAll().stream()
                .map(Sede::getNombre)
                .collect(Collectors.toList());
        
        if (similarityService.existeSimilarEn(sede.getNombre(), nombresExistentes, 0.85)) {
            String similar = similarityService.encontrarSimilarMasCercano(
                sede.getNombre(), nombresExistentes, 0.85);
            throw new RuntimeException("Ya existe una sede con nombre similar: '" + similar + "'");
        }

        sede.setActiva(true);
        return sedeRepository.save(sede);
    }

    @Transactional
    public Sede actualizarSede(Long id, Sede sedeData) {
        Sede existente = sedeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

        // Validar nombre si cambió
        if (sedeData.getNombre() != null && !sedeData.getNombre().isBlank() &&
            !sedeData.getNombre().equals(existente.getNombre())) {
            
            List<String> nombresExistentes = sedeRepository.findAll().stream()
                    .filter(s -> !s.getId().equals(id))
                    .map(Sede::getNombre)
                    .collect(Collectors.toList());
            
            if (similarityService.existeSimilarEn(sedeData.getNombre(), nombresExistentes, 0.85)) {
                String similar = similarityService.encontrarSimilarMasCercano(
                    sedeData.getNombre(), nombresExistentes, 0.85);
                throw new RuntimeException("Ya existe una sede con nombre similar: '" + similar + "'");
            }
            
            existente.setNombre(sedeData.getNombre());
        }

        if (sedeData.getDireccion() != null) existente.setDireccion(sedeData.getDireccion());
        if (sedeData.getDistrito() != null) existente.setDistrito(sedeData.getDistrito());
        if (sedeData.getReferencia() != null) existente.setReferencia(sedeData.getReferencia());
        if (sedeData.getCapacidadMaxima() != null) existente.setCapacidadMaxima(sedeData.getCapacidadMaxima());
        if (sedeData.getTieneEstacionamiento() != null) existente.setTieneEstacionamiento(sedeData.getTieneEstacionamiento());

        return sedeRepository.save(existente);
    }

    @Transactional
    public Sede desactivarSede(Long id) {
        Sede sede = sedeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));
        
        sede.setActiva(false);
        return sedeRepository.save(sede);
    }

    @Transactional
    public void eliminarSede(Long id) {
        Sede sede = sedeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));
        
        // Verificar que no tenga torneos asignados (opcional)
        // Podrías agregar validación adicional aquí
        
        sedeRepository.delete(sede);
    }
}