package com.robotech.repositories;

import com.robotech.models.CodigoRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface CodigoRegistroRepository extends JpaRepository<CodigoRegistro, Long> {
    Optional<CodigoRegistro> findByCodigo(String codigo);
    List<CodigoRegistro> findByUsadoFalse();
    List<CodigoRegistro> findByGeneradoPorId(Long adminId);
    List<CodigoRegistro> findByClubId(Long clubId);
    boolean existsByCodigo(String codigo);
}