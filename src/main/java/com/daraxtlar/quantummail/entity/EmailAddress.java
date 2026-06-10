package com.daraxtlar.quantummail.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="\"email_addresses\"", schema = "qmail")
public class EmailAddress {

    @Id
    @GeneratedValue
    private Long id;

    private Long userId;

    private String emailAddress;

    private String encryptedPassword;

    private String imapHost;

    private Integer imapPort;

    private Boolean sslEnabled;

    private String smtpHost;

    private Integer smtpPort;

    private Boolean smtpSslEnabled;
}
