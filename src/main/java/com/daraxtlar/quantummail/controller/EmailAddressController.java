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

/**
 * REST controller responsible for managing email accounts associated with users.
 *
 * <p>Provides endpoints for adding, retrieving and deleting email account
 * configurations. All operations require a valid JWT token identifying
 * the authenticated user.</p>
 */
@RestController
@RequestMapping("/api/email_accounts")
@CrossOrigin(origins = "http://localhost:5173")
public class EmailAddressController {

    @Autowired
    private EmailAddressService emailAddressService;

    @Autowired
    private JwtService jwtService;

    /**
     * Adds a new email account for the authenticated user.
     *
     * @param bearerToken JWT authorization token
     * @param dto         email account configuration details
     * @return information about the newly created email account
     */
    @PostMapping("/add")
    public ResponseEntity<?> addEmailAccount(
            @RequestHeader("Authorization") String bearerToken,
            @RequestBody EmailAddressDTO dto) {

        try {
            String token = bearerToken.substring(7);
            Long userId = jwtService.getUserIdFromToken(token);

            EmailAddress savedAccount = emailAddressService.addAccount(userId, dto);

            return ResponseEntity.ok(Map.of(
                    "message", "Account added successfully",
                    "accountId", savedAccount.getId(),
                    "email", savedAccount.getEmailAddress()
            ));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", e.getReason()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected error occurred"));
        }

    }

    /**
     * Retrieves all email accounts associated with the authenticated user.
     *
     * @param bearerToken JWT authorization token
     * @return list of configured email accounts
     */
    @GetMapping("/list")
    public ResponseEntity<?> getEmailAccounts(
            @RequestHeader("Authorization") String bearerToken
    ) {
        try {
            String token = bearerToken.substring(7);
            Long userId = jwtService.getUserIdFromToken(token);

            List<EmailAddress> accounts = emailAddressService.getAccountsByUserId(userId);

            return ResponseEntity.ok(accounts);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "An unexpected error occurred"));
        }
    }

    /**
     * Deletes an email account belonging to the authenticated user.
     *
     * @param bearerToken  JWT authorization token
     * @param emailAddress email account address to delete
     * @return operation result information
     */
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteEmailAccount(
            @RequestHeader("Authorization") String bearerToken,
            @RequestParam String emailAddress) {
        try {
            String token = bearerToken.substring(7);
            Long userId = jwtService.getUserIdFromToken(token);

            emailAddressService.deleteAccount(userId, emailAddress);

            return ResponseEntity.ok(Map.of(
                    "message", "Account deleted successfully",
                    "email", emailAddress));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", e.getReason()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

}
