package com.daraxtlar.quantummail.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Setter
@Getter
public class EmailMessage {
    private String id;
    private String from;
    private String to;
    private String subject;
    private String content;
    private String snippet;
    private Date sentDate;
    private boolean read;
    private boolean hasAttachments;
    private List<Attachment> attachments;

    public EmailMessage() {}
}
