package com.daraxtlar.quantummail.service;

import com.daraxtlar.quantummail.entity.User;
import com.daraxtlar.quantummail.model.AuthResponse;
import com.daraxtlar.quantummail.model.LoginRequest;
import com.daraxtlar.quantummail.model.RegisterRequest;
import com.daraxtlar.quantummail.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncodeService encoder;

    public UserService(UserRepository userRepository, PasswordEncodeService encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    public ResponseEntity<AuthResponse> createUser(RegisterRequest request) {

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body(new AuthResponse("Username already exists", false, null));
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(encoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());

        userRepository.save(user);
        return ResponseEntity.ok().body(new AuthResponse("User added sucessfully", true, request.getUsername()));
    }


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

        return ResponseEntity.ok().body(new AuthResponse("User logged in successfully", true, user.getUsername()));
    }
}