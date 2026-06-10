package com.daraxtlar.quantummail.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="\"email_addresses\"", schema = "qmail")
@Getter
@Setter
public class EmailAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
