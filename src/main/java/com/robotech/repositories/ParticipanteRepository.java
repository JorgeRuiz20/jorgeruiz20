package com.robotech.repositories;

import com.robotech.models.Participante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ParticipanteRepository extends JpaRepository<Participante, Long> {
    Optional<Participante> findByUsuarioIdAndTorneoId(Long usuarioId, Long torneoId);

    /**
     * ✅ CORREGIDO: Ahora usa JOIN FETCH para cargar todas las relaciones de una vez
     * Esto previene LazyInitializationException y reduce queries a la BD
     */
    @Query("SELECT DISTINCT p FROM Participante p " +
            "LEFT JOIN FETCH p.usuario u " +
            "LEFT JOIN FETCH u.club " +
            "LEFT JOIN FETCH p.robot r " +
            "LEFT JOIN FETCH r.categoria " +
            "WHERE p.torneo.id = :torneoId " +
            "ORDER BY p.puntuacionTotal DESC, p.partidosGanados DESC, p.partidosEmpatados DESC")
    List<Participante> findByTorneoIdOrderByPuntuacionTotalDesc(@Param("torneoId") Long torneoId);

    @Query("SELECT COUNT(p) FROM Participante p WHERE p.torneo.id = :torneoId")
    Long countByTorneoId(@Param("torneoId") Long torneoId);

    // ✅ NUEVO: Método para obtener participantes de un torneo y club específico
    List<Participante> findByTorneoIdAndUsuario_ClubId(Long torneoId, Long clubId);
}