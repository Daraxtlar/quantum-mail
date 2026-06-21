package com.daraxtlar.quantummail.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

/**
 * Represents a complete email message returned by the application.
 *
 * <p>Contains message metadata, content and optional attachment information.</p>
 */
@Setter
@Getter
public class EmailMessage {

    /**
     * Unique message identifier.
     */
    private String id;

    /**
     * Sender email address.
     */
    private String from;

    /**
     * Recipient email address.
     */
    private String to;

    /**
     * Email subject.
     */
    private String subject;

    /**
     * Full email content.
     */
    private String content;

    /**
     * Short preview of the message content.
     */
    private String snippet;

    /**
     * Date and time when the email was sent.
     */
    private Date sentDate;

    /**
     * Indicates whether the message has been read.
     */
    private boolean read;

    /**
     * Indicates whether the message contains attachments.
     */
    private boolean hasAttachments;

    /**
     * Collection of email attachments.
     */
    private List<Attachment> attachments;

    /**
     * Creates an empty email message instance.
     */
    public EmailMessage() {
    }
}