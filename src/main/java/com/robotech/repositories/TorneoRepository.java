package com.robotech.repositories;

import com.robotech.models.Torneo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TorneoRepository extends JpaRepository<Torneo, Long> {
    List<Torneo> findByEstado(String estado);
    List<Torneo> findByCategoriaId(Long categoriaId);
    boolean existsByNombre(String nombre);
}