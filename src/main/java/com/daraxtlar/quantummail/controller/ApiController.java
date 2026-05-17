package com.daraxtlar.quantummail.controller;

import com.daraxtlar.quantummail.model.LoginRequest;
import com.daraxtlar.quantummail.model.RegisterRequest;
import com.daraxtlar.quantummail.repository.UserRepository;
import com.daraxtlar.quantummail.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class ApiController {


    private final UserService userService;

    public ApiController(UserService userService){
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request){
        return userService.loginUser(request.getUsername(), request.getPassword());
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request){
        return userService.createUser(request.getUsername(), request.getPassword(), request.getEmail());
    }
}