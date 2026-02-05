package com.robotech.repositories;

import com.robotech.models.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findByActivaTrue();
    Optional<Categoria> findByNombre(String nombre);
}