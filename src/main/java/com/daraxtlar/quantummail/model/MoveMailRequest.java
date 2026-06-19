package com.daraxtlar.quantummail.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class MoveMailRequest {
    private String accountEmail;
    private String sourceFolderName;
    private String targetFolderName;
    private long uid;
    private String sender;
    private String subject;
    private Date sentDate;
}
