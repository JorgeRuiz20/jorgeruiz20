package com.robotech.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
public class PasswordResetToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private Boolean usado = false;

    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaUso;

    public PasswordResetToken() {
        this.fechaCreacion = LocalDateTime.now();
        this.expiryDate = LocalDateTime.now().plusHours(24); // Token válido por 24 horas
    }

    public PasswordResetToken(String token, User user) {
        this.token = token;
        this.user = user;
        this.fechaCreacion = LocalDateTime.now();
        this.expiryDate = LocalDateTime.now().plusHours(24);
        this.usado = false;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }
}