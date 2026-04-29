package com.daraxtlar.quantummail.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SendEmailService {

    @Autowired
    private JavaMailSenderImpl mailSender;

    public Boolean sendEmail(String senders, String[] recipients, String subject, String text, String method, MultipartFile[] files) {

        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            System.out.println(recipients);

            helper.setFrom(String.valueOf(senders));
            helper.setTo(recipients);
            helper.setSubject(subject);
            helper.setText(text);

            if (files != null) {
                for (MultipartFile file : files)
                    helper.addAttachment(file.getOriginalFilename(), file);
            }

            mailSender.send(message);
            return true;
        } catch (MessagingException me) {
            System.out.println(senders);
            System.out.println(recipients);
            System.out.println(subject);
            System.out.println(text);
            System.out.println(me);
        }
        return false;
    }
}