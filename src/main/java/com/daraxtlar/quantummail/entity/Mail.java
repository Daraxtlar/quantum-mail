package com.daraxtlar.quantummail.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "mail", schema = "qmail")
@Getter
@Setter
public class Mail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String senderEmail;
    private String recipientEmail;
    private LocalDateTime sentDate;
}
