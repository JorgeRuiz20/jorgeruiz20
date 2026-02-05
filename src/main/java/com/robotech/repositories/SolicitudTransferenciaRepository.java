package com.robotech.repositories;

import com.robotech.models.SolicitudTransferencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SolicitudTransferenciaRepository extends JpaRepository<SolicitudTransferencia, Long> {
    
    // Solicitudes del usuario (todas)
    List<SolicitudTransferencia> findByUsuarioIdOrderByFechaSolicitudDesc(Long usuarioId);
    
    // Solicitudes pendientes de salida del club origen
    @Query("SELECT s FROM SolicitudTransferencia s WHERE s.clubOrigen.id = :clubId AND s.estado = 'PENDIENTE_SALIDA'")
    List<SolicitudTransferencia> findPendientesSalidaByClubOrigen(@Param("clubId") Long clubId);
    
    // Solicitudes pendientes de ingreso al club destino
    @Query("SELECT s FROM SolicitudTransferencia s WHERE s.clubDestino.id = :clubId AND s.estado = 'PENDIENTE_INGRESO'")
    List<SolicitudTransferencia> findPendientesIngresoByClubDestino(@Param("clubId") Long clubId);
    
    // Verificar si el usuario ya tiene una solicitud pendiente
    @Query("SELECT s FROM SolicitudTransferencia s WHERE s.usuario.id = :usuarioId " +
           "AND (s.estado = 'PENDIENTE_SALIDA' OR s.estado = 'PENDIENTE_INGRESO')")
    Optional<SolicitudTransferencia> findSolicitudPendienteByUsuario(@Param("usuarioId") Long usuarioId);
    
    // Todas las solicitudes del club (como origen o destino)
    @Query("SELECT s FROM SolicitudTransferencia s WHERE " +
           "s.clubOrigen.id = :clubId OR s.clubDestino.id = :clubId " +
           "ORDER BY s.fechaSolicitud DESC")
    List<SolicitudTransferencia> findByClubOrigenOrClubDestino(@Param("clubId") Long clubId);
}