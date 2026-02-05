package com.robotech.repositories;

import com.robotech.models.Enfrentamiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface EnfrentamientoRepository extends JpaRepository<Enfrentamiento, Long> {

    // ✅ NUEVO: Buscar por torneo directamente
    @Query("SELECT e FROM Enfrentamiento e WHERE e.torneo.id = :torneoId")
    List<Enfrentamiento> findByTorneoId(@Param("torneoId") Long torneoId);

    // ✅ NUEVO: Buscar por torneo y ronda
    @Query("SELECT e FROM Enfrentamiento e WHERE e.torneo.id = :torneoId AND e.ronda = :ronda")
    List<Enfrentamiento> findByTorneoIdAndRonda(@Param("torneoId") Long torneoId, @Param("ronda") String ronda);

    // DEPRECADO:
    @Deprecated
    @Query("SELECT e FROM Enfrentamiento e WHERE e.torneo.id = :competenciaId")
    List<Enfrentamiento> findByCompetenciaId(@Param("competenciaId") Long competenciaId);

    @Deprecated
    @Query("SELECT e FROM Enfrentamiento e WHERE e.torneo.id = :competenciaId AND e.ronda = :ronda")
    List<Enfrentamiento> findByCompetenciaIdAndRonda(@Param("competenciaId") Long competenciaId,
            @Param("ronda") String ronda);

    // ✅ NUEVO: Contar enfrentamientos por torneo
    @Query("SELECT COUNT(e) FROM Enfrentamiento e WHERE e.torneo.id = :torneoId")
    int countByTorneoId(@Param("torneoId") Long torneoId);
}