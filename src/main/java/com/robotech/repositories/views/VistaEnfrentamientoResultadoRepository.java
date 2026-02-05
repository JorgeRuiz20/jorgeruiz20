package com.robotech.repositories.views;

import com.robotech.models.views.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;


public interface VistaEnfrentamientoResultadoRepository extends JpaRepository<VistaEnfrentamientoResultado, Long> {
    
    List<VistaEnfrentamientoResultado> findByTorneoId(Long torneoId);
    
    List<VistaEnfrentamientoResultado> findByTorneoIdAndRonda(Long torneoId, String ronda);
    
    List<VistaEnfrentamientoResultado> findByResultado(String resultado);
    
    @Query("SELECT v FROM VistaEnfrentamientoResultado v WHERE v.participante1Club = :club OR v.participante2Club = :club")
    List<VistaEnfrentamientoResultado> findByClubNombre(@Param("club") String clubNombre);
    
    @Query("SELECT v FROM VistaEnfrentamientoResultado v WHERE v.fechaEnfrentamiento BETWEEN :inicio AND :fin")
    List<VistaEnfrentamientoResultado> findByFechaBetween(
        @Param("inicio") LocalDateTime inicio, 
        @Param("fin") LocalDateTime fin
    );
}