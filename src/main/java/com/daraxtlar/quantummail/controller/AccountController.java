package com.daraxtlar.quantummail.controller;

import com.daraxtlar.quantummail.model.DeleteAccountRequest;
import com.daraxtlar.quantummail.model.PasswordChangeRequest;
import com.daraxtlar.quantummail.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * REST controller responsible for user account management operations.
 *
 * <p>Provides endpoints for changing user passwords and deleting user accounts.
 * All operations require an authenticated user.</p>
 */
@RestController
@RequestMapping("/api/account")
@CrossOrigin(origins = "http://localhost:5173")
public class AccountController {

    private final UserService userService;

    /**
     * Creates a new account controller.
     *
     * @param userService service responsible for user account operations
     */
    public AccountController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Changes the password of the currently authenticated user.
     *
     * @param request   password change request containing old and new passwords
     * @param principal authenticated user principal
     * @return response indicating whether the password was successfully changed
     */
    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @RequestBody PasswordChangeRequest request,
            Principal principal) {
        return userService.changePassword(
                principal.getName(),
                request.getOldPassword(),
                request.getNewPassword());
    }

    /**
     * Deletes the account of the currently authenticated user.
     *
     * @param request   account deletion request containing the user's password
     * @param principal authenticated user principal
     * @return response indicating whether the account was successfully deleted
     */
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteAccount(
            @RequestBody DeleteAccountRequest request,
            Principal principal) {
        return userService.deleteAccount(
                principal.getName(),
                request.getPassword());
    }
}
