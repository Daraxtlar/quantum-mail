package com.daraxtlar.quantummail.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents metadata describing an email attachment.
 *
 * <p>Contains basic information about an attached file, including
 * its name, content type and size.</p>
 */
@Setter
@Getter
@AllArgsConstructor
public class Attachment {

    /**
     * Attachment filename.
     */
    private String fileName;

    /**
     * MIME content type of the attachment.
     */
    private String contentType;

    /**
     * Attachment size in bytes.
     */
    private long size;
}