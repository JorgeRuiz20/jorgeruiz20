package com.robotech.repositories;

import com.robotech.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByClubIdAndEstado(Long clubId, String estado);
    List<User> findByEstado(String estado);
    boolean existsByEmail(String email);
    boolean existsByDni(String dni);
    Optional<User> findByDni(String dni);
}