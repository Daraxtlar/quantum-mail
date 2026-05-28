package com.daraxtlar.quantummail.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class Attachment {
    private String fileName;
    private String contentType;
    private long size;
}
