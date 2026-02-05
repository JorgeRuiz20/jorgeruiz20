package com.robotech.repositories.views;

import com.robotech.models.views.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface VistaEstadisticasClubRepository extends JpaRepository<VistaEstadisticasClub, Long> {
    
    List<VistaEstadisticasClub> findByCiudad(String ciudad);
    
    List<VistaEstadisticasClub> findByPais(String pais);
    
    @Query("SELECT v FROM VistaEstadisticasClub v WHERE v.torneosGanados > 0 ORDER BY v.torneosGanados DESC")
    List<VistaEstadisticasClub> findClubsConTorneosGanados();
    
    @Query("SELECT v FROM VistaEstadisticasClub v ORDER BY v.puntuacionAcumulada DESC")
    List<VistaEstadisticasClub> findAllOrderByPuntuacionDesc();
    
    @Query("SELECT v FROM VistaEstadisticasClub v WHERE v.totalMiembros >= :minMiembros")
    List<VistaEstadisticasClub> findByTotalMiembrosGreaterThan(@Param("minMiembros") Long minMiembros);
}