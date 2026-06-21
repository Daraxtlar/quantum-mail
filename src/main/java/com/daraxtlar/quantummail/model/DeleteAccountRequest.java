package com.daraxtlar.quantummail.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Request object used for account deletion operations.
 *
 * <p>Contains the user's password for confirmation purposes.</p>
 */
@Getter
@Setter
public class DeleteAccountRequest {

    /**
     * User password used to confirm account deletion.
     */
    private String password;
}
