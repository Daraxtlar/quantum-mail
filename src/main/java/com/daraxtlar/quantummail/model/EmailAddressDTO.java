package com.daraxtlar.quantummail.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Data transfer object containing email account configuration details.
 *
 * <p>Used when creating or updating external email account settings.</p>
 */
@Getter
@Setter
public class EmailAddressDTO {

    /**
     * Email account address.
     */
    private String emailAddress;

    /**
     * Account password.
     */
    private String password;

    /**
     * IMAP server hostname.
     */
    private String imapHost;

    /**
     * IMAP server port number.
     */
    private Integer imapPort;

    /**
     * Indicates whether SSL is enabled for IMAP communication.
     */
    private Boolean sslEnabled;

    /**
     * SMTP server hostname.
     */
    private String smtpHost;

    /**
     * SMTP server port number.
     */
    private Integer smtpPort;

    /**
     * Indicates whether SSL is enabled for SMTP communication.
     */
    private Boolean smtpSslEnabled;
}