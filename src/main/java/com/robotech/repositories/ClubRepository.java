package com.robotech.repositories;

import com.robotech.models.Club;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface ClubRepository extends JpaRepository<Club, Long> {
    Optional<Club> findByOwnerId(Long ownerId);
    List<Club> findByNombreContainingIgnoreCase(String nombre);
    
    @Query("SELECT c FROM Club c WHERE c.owner.id = :ownerId")
    Optional<Club> findClubByOwner(Long ownerId);
    
 // En ClubRepository.java
    boolean existsByNombreIgnoreCase(String nombre);
}