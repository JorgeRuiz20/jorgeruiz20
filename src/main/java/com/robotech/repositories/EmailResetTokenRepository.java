package com.robotech.repositories;

import com.robotech.models.EmailResetToken;
import com.robotech.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailResetTokenRepository extends JpaRepository<EmailResetToken, Long> {
    
    Optional<EmailResetToken> findByToken(String token);
    
    Optional<EmailResetToken> findByUser(User user);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM EmailResetToken e WHERE e.expiryDate < ?1")
    void deleteAllExpiredTokens(LocalDateTime now);
    
    @Modifying
    @Transactional
    void deleteByUser(User user);
}