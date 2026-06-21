package com.daraxtlar.quantummail.service;

import com.daraxtlar.quantummail.entity.User;
import com.daraxtlar.quantummail.model.AuthResponse;
import com.daraxtlar.quantummail.model.LoginRequest;
import com.daraxtlar.quantummail.model.RegisterRequest;
import com.daraxtlar.quantummail.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service responsible for user account management and authentication.
 *
 * <p>Provides functionality for user registration, authentication,
 * password management and account deletion. Passwords are securely
 * stored using BCrypt hashing and authenticated users receive
 * JWT tokens for subsequent requests.</p>
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncodeService encoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository, PasswordEncodeService encoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    /**
     * Creates a new user account.
     *
     * <p>The method validates username uniqueness, hashes the provided
     * password and stores the new user in the database.</p>
     *
     * @param request registration request containing user information
     * @return response containing the registration result
     */
    public ResponseEntity<AuthResponse> createUser(RegisterRequest request) {

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body(new AuthResponse("Username already exists", false, null, null));
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(encoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());

        userRepository.save(user);
        return ResponseEntity.ok().body(new AuthResponse("User added sucessfully", true, request.getUsername()));
    }

    /**
     * Authenticates a user and generates a JWT access token.
     *
     * <p>The provided credentials are validated against the stored
     * password hash. Upon successful authentication a JWT token
     * containing user information is generated.</p>
     *
     * @param request login request containing user credentials
     * @return authentication result and generated JWT token
     */
    public ResponseEntity<AuthResponse> loginUser(LoginRequest request) {

        Optional<User> loggingUser = userRepository.findByUsername(request.getUsername());

        if (loggingUser.isEmpty()) {
            return ResponseEntity.badRequest().body(new AuthResponse("No such user", false));
        }

        User user = loggingUser.get();

        boolean valid = encoder.matches(request.getPassword(), user.getPassword());

        if (!valid) {
            return ResponseEntity.status(401).body(new AuthResponse("Invalid password", false));
        }

        String token = jwtService.generateToken(request.getUsername(), user.getId());

        return ResponseEntity.ok().body(new AuthResponse("User logged in successfully", true, user.getUsername(), token));
    }

    /**
     * Changes the password of an existing user account.
     *
     * <p>The current password must be provided and successfully
     * validated before the new password is stored.</p>
     *
     * @param username    authenticated username
     * @param oldPassword current account password
     * @param newPassword new password to be stored
     * @return operation result
     */
    public ResponseEntity<String> changePassword(String username, String oldPassword, String newPassword) {
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isEmpty()) return ResponseEntity.badRequest().body("User not found");

        User user = userOptional.get();

        if (!encoder.matches(oldPassword, user.getPassword())) {
            return ResponseEntity.status(401).body("Old password does not match");
        }

        user.setPassword(encoder.encode(newPassword));
        userRepository.save(user);
        return ResponseEntity.ok().body("Password changed successfully");
    }

    /**
     * Permanently deletes a user account.
     *
     * <p>The account password must be verified before the account
     * is removed from the database.</p>
     *
     * @param username account username
     * @param password account password
     * @return operation result
     */
    public ResponseEntity<String> deleteAccount(String username, String password) {
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            if (!encoder.matches(password, user.getPassword())) {
                return ResponseEntity.status(401).body("Password does not match");
            }

            userRepository.delete(user);
            return ResponseEntity.ok("User deleted successfully");
        }
        return ResponseEntity.badRequest().body("User not found");
    }
}