package com.daraxtlar.quantummail.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * Request object used for moving an email message between folders.
 *
 * <p>Contains message identification data and source/target folder
 * information required to perform the operation.</p>
 */
@Getter
@Setter
public class MoveMailRequest {

    /**
     * Email account containing the message.
     */
    private String accountEmail;

    /**
     * Source folder from which the message should be moved.
     */
    private String sourceFolderName;

    /**
     * Destination folder to which the message should be moved.
     */
    private String targetFolderName;

    /**
     * Unique identifier of the email message.
     */
    private long uid;

    /**
     * Sender email address.
     */
    private String sender;

    /**
     * Message subject.
     */
    private String subject;

    /**
     * Date and time when the message was sent.
     */
    private Date sentDate;
}