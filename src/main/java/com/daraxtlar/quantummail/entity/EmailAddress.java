package com.daraxtlar.quantummail.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing an email account configuration associated with a user.
 *
 * <p>Stores connection settings required for accessing external mail servers
 * through IMAP and SMTP protocols, including encrypted authentication data.</p>
 */
@Entity
@Table(name = "\"email_addresses\"", schema = "qmail")
@Getter
@Setter
public class EmailAddress {

    /**
     * Unique identifier of the email account configuration.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identifier of the user who owns the email account.
     */
    private Long userId;

    /**
     * Email address associated with the account.
     */
    private String emailAddress;

    /**
     * Encrypted password used for authentication.
     */
    private String encryptedPassword;

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