package com.robotech.dto;

import lombok.Data;

@Data
public class PasswordResetResponse {
    private String message;
    private Boolean success;
    
    public PasswordResetResponse(String message, Boolean success) {
        this.message = message;
        this.success = success;
    }
}