package com.daraxtlar.quantummail.controller;

import com.daraxtlar.quantummail.model.AuthResponse;
import com.daraxtlar.quantummail.model.LoginRequest;
import com.daraxtlar.quantummail.model.RegisterRequest;
import com.daraxtlar.quantummail.repository.UserRepository;
import com.daraxtlar.quantummail.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class ApiController {


    private final UserService userService;

    public ApiController(UserService userService){
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request){
        return userService.loginUser(request);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request){
        return userService.createUser(request);
    }
}