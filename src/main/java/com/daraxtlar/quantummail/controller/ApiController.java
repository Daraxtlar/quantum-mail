package com.daraxtlar.quantummail.controller;

import com.daraxtlar.quantummail.model.AuthResponse;
import com.daraxtlar.quantummail.model.LoginRequest;
import com.daraxtlar.quantummail.model.RegisterRequest;
import com.daraxtlar.quantummail.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 * REST controller responsible for authentication and user registration.
 *
 * <p>Provides endpoints for user login and account creation.</p>
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class ApiController {

    private final UserService userService;

    /**
     * Creates a new authentication controller.
     *
     * @param userService service responsible for authentication and registration
     */
    public ApiController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Authenticates a user and generates a JWT token.
     *
     * @param request login request containing user credentials
     * @return authentication response with login status and token information
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return userService.loginUser(request);
    }

    /**
     * Creates a new user account.
     *
     * @param request registration request containing user details
     * @return authentication response containing registration result
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return userService.createUser(request);
    }
}