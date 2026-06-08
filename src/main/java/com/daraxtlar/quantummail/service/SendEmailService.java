package com.daraxtlar.quantummail.service;

import jakarta.mail.internet.MimeMessage;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.converter.EmailConverter;
import org.simplejavamail.email.EmailBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;


@Service
public class SendEmailService {

    @Autowired
    private JavaMailSenderImpl mailSender;

    @Autowired
    private MailService mailService;

    public Boolean sendEmail(String senders, String[] recipients, String subject, String text, String method, MultipartFile[] files, String folderName, Long parentMailId, String actionType) {

        try {
            Email email;
            String formattedComment = text != null ? text.replace("\n", "<br/>") : "";

            if (parentMailId != null && folderName != null && !folderName.isEmpty()) {
                Email baseEmail = mailService.prepareBaseEmailFromImap(folderName, parentMailId);

                if (baseEmail != null) {
                    var builder = "forward".equalsIgnoreCase(actionType)
                            ? EmailBuilder.forwarding(baseEmail)
                            : EmailBuilder.replyingTo(baseEmail);

                    builder.from(senders).clearRecipients();


                    if (recipients != null) {
                        for (String recipient : recipients) {
                            builder.to(recipient);
                        }
                    }

                    builder.withSubject(subject)
                            .prependTextHTML(formattedComment + "<br><br>");

                    if (files != null) {
                        for (MultipartFile file : files) {
                            builder.withAttachment(file.getOriginalFilename(), file.getBytes(), Objects.requireNonNull(file.getContentType()));
                        }
                    }
                    email = builder.buildEmail();
                }else {
                    return false;
                }
            }else {
                var builder = EmailBuilder.startingBlank()
                        .from(senders)
                        .withSubject(subject)
                        .withHTMLText(formattedComment);

                if (recipients != null) {
                    for (String recipient : recipients) {
                        builder.to(recipient);
                    }
                }

                if (files != null) {
                    for (MultipartFile file : files) {
                        if (!file.isEmpty()) {
                            builder.withAttachment(file.getOriginalFilename(), file.getBytes(), Objects.requireNonNull(file.getContentType()));
                        }
                    }
                }
                email = builder.buildEmail();
            }

            MimeMessage mimeMessage = EmailConverter.emailToMimeMessage(email, mailSender.getSession());
            mailSender.send(mimeMessage);
            return true;
        } catch (Exception e) {
            System.out.println("Błąd podczas wysyłania przez Simple Java Mail:");
            e.printStackTrace();
        }
        return false;
    }
}