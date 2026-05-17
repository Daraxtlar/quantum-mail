package com.daraxtlar.quantummail.service;

import com.daraxtlar.quantummail.entity.User;
import com.daraxtlar.quantummail.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncodeService encoder;

    public UserService(UserRepository userRepository, PasswordEncodeService encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    public ResponseEntity<String> createUser(String username, String password, String email) {

        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(encoder.encode(password));
        user.setEmail(email);

        userRepository.save(user);
        return ResponseEntity.ok().body("User added");
    }

    public ResponseEntity<String> loginUser(String username, String password){

        Optional<User> loggingUser = userRepository.findByUsername(username);

        if (loggingUser.isEmpty()) {
            return ResponseEntity.badRequest().body("No such user");
        }

        User user = loggingUser.get();

        boolean valid = encoder.matches(password, user.getPassword());

        if (!valid) {
            return ResponseEntity.status(401).body("Invalid password");
        }

        return ResponseEntity.ok().body("User logged " + loggingUser);
    }
}