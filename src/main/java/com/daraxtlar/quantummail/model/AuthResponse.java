package com.daraxtlar.quantummail.model;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthResponse {
    private String message;
    private boolean success;
    private String username;

    public AuthResponse(String message, boolean success, String username) {
        this.message = message;
        this.success = success;
        this.username = username;
    }

    public AuthResponse(String message, boolean success) {
        this.message = message;
        this.success = success;
        this.username = null;
    }
}
