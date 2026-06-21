package com.daraxtlar.quantummail.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * Entity representing an email message synchronized from an IMAP server.
 *
 * <p>Stores metadata required for displaying mailbox contents and
 * performing email management operations.</p>
 */
@Entity
@Table(
        name = "imap_mails",
        schema = "qmail",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"uid", "accountEmail", "folderName"})
        }
)
@Getter
@Setter
public class ImapMail {

    /**
     * Unique identifier of the stored message record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique message identifier assigned by the mail server.
     */
    private String uid;

    /**
     * Email account from which the message was synchronized.
     */
    private String accountEmail;

    /**
     * Mailbox folder containing the message.
     */
    private String folderName;

    /**
     * Sender email address.
     */
    private String sender;

    /**
     * Message subject.
     */
    private String subject;

    /**
     * Short preview of the message content.
     */
    @Column(length = 500)
    private String snippet;

    /**
     * Date and time when the message was sent.
     */
    @Temporal(TemporalType.TIMESTAMP)
    private Date sentDate;

    /**
     * Indicates whether the message has been marked as read.
     */
    private boolean isRead;

    /**
     * Indicates whether the message has been marked as starred.
     */
    private boolean isStarred;
}