package com.daraxtlar.quantummail.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing an application user.
 *
 * <p>Stores authentication and identification data required
 * for accessing the system.</p>
 */
@Entity
@Table(name = "\"user\"", schema = "qmail")
@Getter
@Setter
public class User {

    /**
     * Unique identifier of the user.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique username used for authentication.
     */
    @Column(unique = true)
    private String username;

    /**
     * User password stored in encoded form.
     */
    private String password;

    /**
     * User email address.
     */
    private String email;
}