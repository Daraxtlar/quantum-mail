package com.daraxtlar.quantummail.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Request object used during user authentication.
 *
 * <p>Contains the credentials required to log in to the application.</p>
 */
@Getter
@Setter
public class LoginRequest {

    /**
     * Username used for authentication.
     */
    private String username;

    /**
     * User password.
     */
    private String password;
}