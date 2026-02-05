package com.robotech.repositories.views;

import com.robotech.models.views.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;


public interface VistaParticipanteDetalleRepository extends JpaRepository<VistaParticipanteDetalle, Long> {
    
    List<VistaParticipanteDetalle> findByTorneoId(Long torneoId);
    
    List<VistaParticipanteDetalle> findByClubId(Long clubId);
    
    List<VistaParticipanteDetalle> findByUsuarioId(Long usuarioId);
    
    @Query("SELECT v FROM VistaParticipanteDetalle v WHERE v.torneoId = :torneoId ORDER BY v.puntuacionTotal DESC")
    List<VistaParticipanteDetalle> findByTorneoIdOrderByPuntuacionDesc(@Param("torneoId") Long torneoId);
    
    @Query("SELECT v FROM VistaParticipanteDetalle v WHERE v.efectividad >= :minEfectividad")
    List<VistaParticipanteDetalle> findByEfectividadGreaterThan(@Param("minEfectividad") Double minEfectividad);
}