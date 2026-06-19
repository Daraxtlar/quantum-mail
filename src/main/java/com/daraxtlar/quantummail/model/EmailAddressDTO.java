package com.daraxtlar.quantummail.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailAddressDTO {
    private String emailAddress;
    private String password;
    private String imapHost;
    private Integer imapPort;
    private Boolean sslEnabled;
    private String smtpHost;
    private Integer smtpPort;
    private Boolean smtpSslEnabled;
}
