package com.daraxtlar.quantummail.service;

import com.daraxtlar.quantummail.entity.EmailAddress;
import com.daraxtlar.quantummail.entity.Mail;
import com.daraxtlar.quantummail.repository.EmailAddressRepository;
import com.daraxtlar.quantummail.repository.MailRepository;
import com.daraxtlar.quantummail.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.converter.EmailConverter;
import org.simplejavamail.email.EmailBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Properties;


@Service
public class SendEmailService {

    @Autowired
    private MailService mailService;

    @Autowired
    MailRepository mailRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailAddressRepository emailAddressRepository;

    @Autowired
    private EmailCryptoService emailCryptoService;

    public Boolean sendEmail(Long userId, String senders, String[] recipients, String subject, String text, String method, MultipartFile[] files, String folderName, Long parentMailId, String actionType) {
        try {
            EmailAddress account = emailAddressRepository.findByEmailAddressAndUserId(senders, userId)
                    .orElseThrow(() -> new SecurityException("Sender email not found for user"));

            JavaMailSenderImpl dynamicMailSender = createMailSender(account);

            Email email;
            String formattedComment = text != null ? text.replace("\n", "<br/>") : "";

            if (parentMailId != null && folderName != null && !folderName.isEmpty()) {
                Email baseEmail = mailService.prepareBaseEmailFromImap(userId, senders, folderName, parentMailId);

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

            MimeMessage mimeMessage = EmailConverter.emailToMimeMessage(email, dynamicMailSender.getSession());
            dynamicMailSender.send(mimeMessage);

            for (String recipient : recipients) {
                String cleanRecipient = recipient.trim();
                if (!cleanRecipient.isEmpty()) {
                    Mail contact = mailRepository.findBySenderEmailAndRecipientEmailAndUserId(senders, cleanRecipient, userId).orElse(new Mail());

                    if (contact.getId() == null) {
                        contact.setSenderEmail(senders);
                        contact.setRecipientEmail(cleanRecipient);
                        contact.setUser(userRepository.getReferenceById(userId));
                    }

                    contact.setSentDate(LocalDateTime.now());
                    mailRepository.save(contact);
                }
            }

            return true;
        } catch (Exception e) {
            System.out.println("Błąd podczas wysyłania przez Simple Java Mail:");
            e.printStackTrace();
        }
        return false;
    }

    private JavaMailSenderImpl createMailSender(EmailAddress account) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(account.getSmtpHost());
        sender.setPort(account.getSmtpPort());
        sender.setUsername(account.getEmailAddress());
        sender.setPassword(emailCryptoService.decrypt(account.getEncryptedPassword()));

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");

        if (Boolean.TRUE.equals(account.getSmtpSslEnabled())){
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.port", String.valueOf(account.getSmtpPort()));
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        }else {
            props.put("mail.smtp.starttls.enable", "true");
        }

        props.put("mail.debug", "false");

        return sender;
    }
}