package com.daraxtlar.quantummail.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@Table(name = "imap_mails", schema = "qmail", uniqueConstraints = {@UniqueConstraint(columnNames = {"uid", "accountEmail", "folderName"})})
@Getter
@Setter
public class ImapMail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String uid;
    private String accountEmail;
    private String folderName;

    private String sender;
    private String subject;

    @Column(length = 500)
    private String snippet;

    @Temporal(TemporalType.TIMESTAMP)
    private Date sentDate;

    private boolean isRead;
    private boolean isStarred;
}
