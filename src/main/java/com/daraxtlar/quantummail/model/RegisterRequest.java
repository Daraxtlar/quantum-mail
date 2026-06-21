package com.daraxtlar.quantummail.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Request object used during user registration.
 *
 * <p>Contains the information required to create a new user account.</p>
 */
@Getter
@Setter
public class RegisterRequest {

    /**
     * Desired username.
     */
    private String username;

    /**
     * User password.
     */
    private String password;

    /**
     * User email address.
     */
    private String email;
}