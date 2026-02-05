package com.robotech.repositories;

import com.robotech.models.ClubDeshabilitacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ClubDeshabilitacionRepository extends JpaRepository<ClubDeshabilitacion, Long> {
    
    Optional<ClubDeshabilitacion> findByClubId(Long clubId);
    
    List<ClubDeshabilitacion> findByEstado(String estado);
    
    @Query("SELECT cd FROM ClubDeshabilitacion cd WHERE cd.estado = 'PENDIENTE' " +
           "AND cd.fechaLimiteAccion < :fecha")
    List<ClubDeshabilitacion> findPendientesExpirados(@Param("fecha") LocalDateTime fecha);
    
    @Query("SELECT cd FROM ClubDeshabilitacion cd WHERE cd.club.id = :clubId " +
           "AND cd.estado IN ('PENDIENTE', 'PROCESANDO')")
    Optional<ClubDeshabilitacion> findActivaByClubId(@Param("clubId") Long clubId);
    
    boolean existsByClubIdAndEstadoIn(Long clubId, List<String> estados);
}