package com.daraxtlar.quantummail.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Request object used for password change operations.
 *
 * <p>Contains both the current password for verification and
 * the new password that should be assigned to the account.</p>
 */
@Getter
@Setter
public class PasswordChangeRequest {

    /**
     * User's current password.
     */
    private String oldPassword;

    /**
     * New password to be assigned to the account.
     */
    private String newPassword;
}