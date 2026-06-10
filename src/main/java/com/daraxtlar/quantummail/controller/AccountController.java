package com.daraxtlar.quantummail.controller;

import com.daraxtlar.quantummail.model.DeleteAccountRequest;
import com.daraxtlar.quantummail.model.PasswordChangeRequest;
import com.daraxtlar.quantummail.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/account")
@CrossOrigin(origins = "http://localhost:5173")
public class AccountController {
    private final UserService userService;

    public AccountController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@RequestBody PasswordChangeRequest request, Principal principal){
        return userService.changePassword(principal.getName(), request.getOldPassword(), request.getNewPassword());
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteAccount(@RequestBody DeleteAccountRequest request, Principal principal){
        return userService.deleteAccount(principal.getName(), request.getPassword());
    }
}
