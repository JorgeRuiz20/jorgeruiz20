package com.robotech.services;

import com.robotech.models.Categoria;
import com.robotech.repositories.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoriaService {
    private final CategoriaRepository categoriaRepository;
    private final SimilarityService similarityService;

    public List<Categoria> getCategoriasActivas() {
        return categoriaRepository.findByActivaTrue();
    }

    public Optional<Categoria> getCategoriaById(Long id) {
        return categoriaRepository.findById(id);
    }

    public Categoria crearCategoria(Categoria categoria) {
        // ✅ Validación con Jaro-Winkler (umbral 85% - estricto para categorías)
        String nombreNuevo = categoria.getNombre();
        if (nombreNuevo == null || nombreNuevo.isBlank()) {
            throw new RuntimeException("El nombre de la categoría no puede estar vacío");
        }
        
        List<String> nombresExistentes = categoriaRepository.findAll().stream()
                .map(Categoria::getNombre)
                .collect(Collectors.toList());
        
        if (similarityService.existeCategoriaSimilar(nombreNuevo, nombresExistentes)) {
            String similar = similarityService.encontrarCategoriaSimilar(nombreNuevo, nombresExistentes);
            throw new RuntimeException("Ya existe una categoría con nombre similar: '" + similar + "'");
        }
        
        categoria.setActiva(true);
        return categoriaRepository.save(categoria);
    }

    public Categoria actualizarCategoria(Long id, Categoria categoria) {
        Categoria existente = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
        
        // ✅ Validación si se cambia el nombre
        if (categoria.getNombre() != null && !categoria.getNombre().isBlank() && 
            !categoria.getNombre().equals(existente.getNombre())) {
            
            List<String> nombresExistentes = categoriaRepository.findAll().stream()
                    .filter(c -> !c.getId().equals(id))
                    .map(Categoria::getNombre)
                    .collect(Collectors.toList());
            
            if (similarityService.existeCategoriaSimilar(categoria.getNombre(), nombresExistentes)) {
                String similar = similarityService.encontrarCategoriaSimilar(categoria.getNombre(), nombresExistentes);
                throw new RuntimeException("Ya existe una categoría con nombre similar: '" + similar + "'");
            }
            
            existente.setNombre(categoria.getNombre());
        }
        
        existente.setDescripcion(categoria.getDescripcion());
        existente.setEdadMinima(categoria.getEdadMinima());
        existente.setEdadMaxima(categoria.getEdadMaxima());
        existente.setPesoMaximo(categoria.getPesoMaximo());
        existente.setReglasEspecificas(categoria.getReglasEspecificas());
        
        return categoriaRepository.save(existente);
    }

    public Categoria desactivarCategoria(Long id) {
        Categoria existente = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
        
        existente.setActiva(false);
        return categoriaRepository.save(existente);
    }
}