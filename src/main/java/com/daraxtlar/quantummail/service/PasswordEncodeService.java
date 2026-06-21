package com.daraxtlar.quantummail.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service responsible for password hashing and verification.
 *
 * <p>Uses the BCrypt algorithm to securely store user passwords
 * and validate authentication attempts.</p>
 */
@Service
public class PasswordEncodeService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * Generates a BCrypt hash for the provided password.
     *
     * @param password plain-text password
     * @return encoded password hash
     */
    public String encode(String password) {
        return encoder.encode(password);
    }

    /**
     * Verifies whether a plain-text password matches a stored hash.
     *
     * @param rawPassword plain-text password
     * @param hash        stored password hash
     * @return {@code true} if the password matches the hash,
     * otherwise {@code false}
     */
    public boolean matches(String rawPassword, String hash) {
        return encoder.matches(rawPassword, hash);
    }
}