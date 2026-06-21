package com.daraxtlar.quantummail.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity representing a suggested recipient relationship used for
 * email auto-completion and recipient recommendations.
 *
 * <p>The entity stores information about previously used sender-recipient
 * combinations for a specific user.</p>
 */
@Entity
@Table(name = "mail", schema = "qmail")
@Getter
@Setter
public class Mail {

    /**
     * Unique identifier of the suggestion record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User associated with the suggestion.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Sender email address.
     */
    private String senderEmail;

    /**
     * Recipient email address.
     */
    private String recipientEmail;

    /**
     * Date and time when the sender-recipient relation was recorded.
     */
    private LocalDateTime sentDate;
}