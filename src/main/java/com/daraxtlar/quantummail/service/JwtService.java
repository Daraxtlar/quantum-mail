package com.daraxtlar.quantummail.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * Service responsible for generating and validating JSON Web Tokens (JWT).
 *
 * <p>Tokens are used to authenticate users and carry basic identity
 * information such as username and user identifier.</p>
 */
@Service
public class JwtService {
    private final String secret = "mysecretkey_projekt_quantummail_java_project_piu";

    /**
     * Creates the cryptographic signing key used for JWT operations.
     *
     * @return JWT signing key
     */
    private Key getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a JWT token for an authenticated user.
     *
     * @param username authenticated username
     * @param userId   authenticated user identifier
     * @return signed JWT token
     */
    public String generateToken(String username, Long userId) {
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extracts the username stored in a JWT token.
     *
     * @param token JWT token
     * @return username stored within the token
     */
    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /**
     * Extracts the user identifier stored in a JWT token.
     *
     * @param token JWT token
     * @return user identifier stored within the token
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        Number userIdObj = claims.get("userId", Number.class);
        return userIdObj != null ? userIdObj.longValue() : null;
    }
}
