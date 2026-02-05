package com.robotech.repositories.views;

import com.robotech.models.views.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;


public interface VistaRankingGeneralRepository extends JpaRepository<VistaRankingGeneral, Long> {
    
    List<VistaRankingGeneral> findByTorneoId(Long torneoId);
    
    @Query("SELECT v FROM VistaRankingGeneral v WHERE v.torneoId = :torneoId ORDER BY v.posicion ASC")
    List<VistaRankingGeneral> findByTorneoIdOrderByPosicion(@Param("torneoId") Long torneoId);
    
    List<VistaRankingGeneral> findByClubNombre(String clubNombre);
    
    @Query("SELECT v FROM VistaRankingGeneral v WHERE v.posicion <= :limite")
    List<VistaRankingGeneral> findTopByPosicion(@Param("limite") Integer limite);
}