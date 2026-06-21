package com.daraxtlar.quantummail.model;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

/**
 * Response object returned after authentication-related operations.
 *
 * <p>Contains information about the operation result and optionally
 * includes user information and a JWT token.</p>
 */
@Getter
@Setter
public class AuthResponse {

    /**
     * Human-readable operation result message.
     */
    private String message;

    /**
     * Indicates whether the operation completed successfully.
     */
    private boolean success;

    /**
     * Authenticated user's username.
     */
    private String username;

    /**
     * JWT token generated during authentication.
     *
     * <p>The field is omitted from JSON responses when its value is null.</p>
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String token;

    /**
     * Creates a complete authentication response.
     *
     * @param message  operation result message
     * @param success  operation status
     * @param username authenticated username
     * @param token    generated JWT token
     */
    public AuthResponse(String message, boolean success, String username, String token) {
        this.message = message;
        this.success = success;
        this.username = username;
        this.token = token;
    }

    /**
     * Creates a basic authentication response without user information.
     *
     * @param message operation result message
     * @param success operation status
     */
    public AuthResponse(String message, boolean success) {
        this.message = message;
        this.success = success;
        this.username = null;
        this.token = null;
    }

    /**
     * Creates an authentication response containing user information
     * but without a JWT token.
     *
     * @param message  operation result message
     * @param success  operation status
     * @param username username associated with the response
     */
    public AuthResponse(String message, boolean success, String username) {
        this.message = message;
        this.success = success;
        this.username = username;
        this.token = null;
    }
}