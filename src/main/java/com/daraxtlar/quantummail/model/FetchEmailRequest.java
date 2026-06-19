package com.daraxtlar.quantummail.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FetchEmailRequest {
    private String email;
    private String passsword;

    public FetchEmailRequest() {}
}
