package com.robotech.repositories.views;

import com.robotech.models.views.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

// 1️⃣ VistaTorneoCompletaRepository.java
public interface VistaTorneoCompletaRepository extends JpaRepository<VistaTorneoCompleta, Long> {
    
    List<VistaTorneoCompleta> findByTorneoEstado(String estado);
    
    List<VistaTorneoCompleta> findByCategoriaId(Long categoriaId);
    
    @Query("SELECT v FROM VistaTorneoCompleta v WHERE v.fechaCreacion BETWEEN :inicio AND :fin")
    List<VistaTorneoCompleta> findByFechaCreacionBetween(
        @Param("inicio") LocalDateTime inicio, 
        @Param("fin") LocalDateTime fin
    );
    
    @Query("SELECT v FROM VistaTorneoCompleta v WHERE v.torneoNombre LIKE %:nombre%")
    List<VistaTorneoCompleta> findByTorneoNombreContaining(@Param("nombre") String nombre);
}