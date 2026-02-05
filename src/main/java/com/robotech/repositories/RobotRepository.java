package com.robotech.repositories;

import com.robotech.models.Robot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface RobotRepository extends JpaRepository<Robot, Long> {
    List<Robot> findByUsuarioId(Long usuarioId);

    List<Robot> findByCategoriaIdAndEstado(Long categoriaId, String estado);

    Optional<Robot> findByCodigoIdentificacion(String codigo);

    @Query("SELECT r FROM Robot r WHERE r.usuario.club.id = :clubId AND r.estado = 'APROBADO'")
    List<Robot> findRobotsAprobadosByClub(Long clubId);

    @Query("SELECT r FROM Robot r WHERE r.usuario.club.owner.id = :clubOwnerId")
    List<Robot> findRobotsByClubOwner(Long clubOwnerId);

    // ✅ NUEVA QUERY: Obtener robots pendientes para un club owner
    // Incluye robots de usuarios que ya están en el club O que tienen solicitud
    // pendiente
@Query("SELECT r FROM Robot r WHERE r.estado = 'PENDIENTE' AND r.usuario.club.id = :clubId")
List<Robot> findRobotsPendientesByClub(@Param("clubId") Long clubId);

}