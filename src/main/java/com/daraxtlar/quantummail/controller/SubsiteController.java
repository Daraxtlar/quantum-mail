package com.daraxtlar.quantummail.controller;

import com.daraxtlar.quantummail.service.SendEmailService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class SubsiteController {

    private final SendEmailService sendEmailService;

    public SubsiteController(SendEmailService sendEmailService) {
        this.sendEmailService = sendEmailService;
    }

    // temporary - for experimenting on sending email
    @GetMapping("/")
    public String homePage() {
        return "index";
    }

    @PostMapping("/sendEmail")
    public String sendEmail(@RequestParam String[] to,
                            @RequestParam String subject,
                            @RequestParam String text,
                            @RequestParam MultipartFile[] files) {

        sendEmailService.sendEmail("alicja.naw04@gmail.com", to, subject, text, "cc", files);

        return "index";
    }
}