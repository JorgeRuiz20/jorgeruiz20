package com.robotech.repositories;

import com.robotech.models.Sede;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SedeRepository extends JpaRepository<Sede, Long> {
    
    List<Sede> findByActivaTrue();
    
    Optional<Sede> findByNombre(String nombre);
    
    List<Sede> findByDistrito(String distrito);
    
    boolean existsByNombre(String nombre);
}