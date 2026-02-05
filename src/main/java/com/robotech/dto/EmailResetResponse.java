package com.robotech.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
class EmailResetResponse {
    private String message;
    private Boolean success;
    private String nuevoEmail;
    private String temporalPassword;
    
    public EmailResetResponse(String message, Boolean success) {
        this.message = message;
        this.success = success;
    }
    
    public EmailResetResponse(String message, Boolean success, String nuevoEmail, String temporalPassword) {
        this.message = message;
        this.success = success;
        this.nuevoEmail = nuevoEmail;
        this.temporalPassword = temporalPassword;
    }
}