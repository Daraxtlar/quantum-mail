package com.daraxtlar.quantummail.model;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthResponse {
    private String message;
    private boolean success;
    private String username;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String token;

    public AuthResponse(String message, boolean success, String username, String token) {
        this.message = message;
        this.success = success;
        this.username = username;
        this.token = token;
    }

    public AuthResponse(String message, boolean success) {
        this.message = message;
        this.success = success;
        this.username = null;
        this.token = null;
    }

    public AuthResponse(String message, boolean success, String username) {
        this.message = message;
        this.success = success;
        this.username = username;
        this.token = null;
    }
}
