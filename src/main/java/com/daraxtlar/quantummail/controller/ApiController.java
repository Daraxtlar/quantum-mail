package com.daraxtlar.quantummail.controller;

import com.daraxtlar.quantummail.model.LoginRequest;
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

        System.out.println(request.getUsername());
        System.out.println(request.getPassword());

        return ResponseEntity.ok(
                Map.of("message", "login successful")
        );
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody LoginRequest request){

        userService.createUser(request.getUsername(), request.getPassword());

        return ResponseEntity.ok(
                Map.of("message", "register successful")
        );
    }
}