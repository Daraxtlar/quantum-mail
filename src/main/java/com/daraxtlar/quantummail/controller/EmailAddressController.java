package com.daraxtlar.quantummail.controller;

import com.daraxtlar.quantummail.entity.EmailAddress;
import com.daraxtlar.quantummail.model.EmailAddressDTO;
import com.daraxtlar.quantummail.service.EmailAddressService;
import com.daraxtlar.quantummail.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/email_accounts")
@CrossOrigin(origins = "http://localhost:5173")
public class EmailAddressController {

    @Autowired
    private EmailAddressService emailAddressService;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/add")
    public ResponseEntity<?> addEmailAccount(
            @RequestHeader("Authorization") String bearerToken,
            @RequestBody EmailAddressDTO dto){

        try {
            String token = bearerToken.substring(7);
            Long userId = jwtService.getUserIdFromToken(token);

            EmailAddress savedAccount = emailAddressService.addAccount(userId, dto);

            return ResponseEntity.ok(Map.of(
                    "message", "Account added successfully",
                    "accountId", savedAccount.getId(),
                    "email", savedAccount.getEmailAddress()
            ));
        }catch (ResponseStatusException e){
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", e.getReason()));
        }catch (IllegalArgumentException e){
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected error occurred"));
        }

    }

    @GetMapping("/list")
    public ResponseEntity<?> getEmailAccounts(
            @RequestHeader("Authorization") String bearerToken
    ) {
        try {
            String token = bearerToken.substring(7);
            Long userId = jwtService.getUserIdFromToken(token);

            List<EmailAddress> accounts = emailAddressService.getAccountsByUserId(userId);

            return ResponseEntity.ok(accounts);
        }catch (Exception e){
            return ResponseEntity.internalServerError().body(Map.of("error", "An unexpected error occurred"));
        }
    }

}
