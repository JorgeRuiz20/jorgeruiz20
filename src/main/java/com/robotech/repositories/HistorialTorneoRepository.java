package com.robotech.repositories;

import com.robotech.models.HistorialTorneo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface HistorialTorneoRepository extends JpaRepository<HistorialTorneo, Long> {
    
    List<HistorialTorneo> findByTorneoId(Long torneoId);
    
    List<HistorialTorneo> findByUsuarioGanadorId(Long usuarioId);
    
    List<HistorialTorneo> findByUsuarioPerdedorId(Long usuarioId);
    
    List<HistorialTorneo> findByClubGanadorId(Long clubId);
    
    @Query("SELECT h FROM HistorialTorneo h WHERE " +
           "h.usuarioGanador.id = :usuarioId OR h.usuarioPerdedor.id = :usuarioId " +
           "ORDER BY h.fechaEvento DESC")
    List<HistorialTorneo> findHistorialByUsuario(@Param("usuarioId") Long usuarioId);
    
    @Query("SELECT h FROM HistorialTorneo h WHERE " +
           "(h.usuarioGanador.id = :usuarioId1 AND h.usuarioPerdedor.id = :usuarioId2) OR " +
           "(h.usuarioGanador.id = :usuarioId2 AND h.usuarioPerdedor.id = :usuarioId1) " +
           "ORDER BY h.fechaEvento DESC")
    List<HistorialTorneo> findEnfrentamientosEntreUsuarios(
            @Param("usuarioId1") Long usuarioId1, 
            @Param("usuarioId2") Long usuarioId2);
    
    List<HistorialTorneo> findAllByOrderByFechaEventoDesc();
}