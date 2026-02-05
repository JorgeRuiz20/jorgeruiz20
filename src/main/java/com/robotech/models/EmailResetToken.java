package com.robotech.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * ✅ Token para restablecimiento de correo electrónico
 */
@Entity
@Table(name = "email_reset_tokens")
@Getter
@Setter
public class EmailResetToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private String nuevoEmail;
    
    @Column(nullable = false)
    private String nuevaPassword; // Contraseña temporal generada

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private Boolean usado = false;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private User administrador; // Quién hizo el cambio

    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaUso;

    public EmailResetToken() {
        this.fechaCreacion = LocalDateTime.now();
        this.expiryDate = LocalDateTime.now().plusHours(24); // Válido por 24 horas
    }

    public EmailResetToken(String token, User user, String nuevoEmail, String nuevaPassword, User administrador) {
        this.token = token;
        this.user = user;
        this.nuevoEmail = nuevoEmail;
        this.nuevaPassword = nuevaPassword;
        this.administrador = administrador;
        this.fechaCreacion = LocalDateTime.now();
        this.expiryDate = LocalDateTime.now().plusHours(24);
        this.usado = false;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }
}