package com.daraxtlar.quantummail.service;

import com.daraxtlar.quantummail.entity.EmailAddress;
import com.daraxtlar.quantummail.model.EmailAddressDTO;
import com.daraxtlar.quantummail.repository.EmailAddressRepository;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.Transport;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Properties;

@Service
public class EmailAddressService {

    @Autowired
    private EmailAddressRepository emailAddressRepository;

    @Autowired
    private EmailCryptoService emailCryptoService;

    public EmailAddress addAccount(Long userId, EmailAddressDTO dto) {
        if (emailAddressRepository.existsByEmailAddressAndUserId(dto.getEmailAddress(), userId)) {
            throw new IllegalArgumentException("Email address already exists");
        }

        try{
            Properties imapProps = new Properties();
            imapProps.put("mail.store.protocol", "imap");
            imapProps.put("mail.imap.host", dto.getImapHost());
            imapProps.put("mail.imap.port", dto.getImapPort());
            imapProps.put("mail.imap.ssl.enable", dto.getSslEnabled());
            imapProps.put("mail.imap.connectiontimeout", "5000");
            imapProps.put("mail.imap.timeout", "5000");

            Session imapSession = Session.getInstance(imapProps);
            Store store = imapSession.getStore("imap");
            store.connect(dto.getEmailAddress(), dto.getPassword());
            store.close();
        }catch (Exception e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IMAP Configuration Error: " + e.getMessage());
        }

        try{
            Properties smtpProps = new Properties();
            smtpProps.put("mail.transport.protocol", "smtp");
            smtpProps.put("mail.smtp.host", dto.getSmtpHost());
            smtpProps.put("mail.smtp.port", dto.getSmtpPort());
            smtpProps.put("mail.smtp.auth", "true");
            smtpProps.put("mail.smtp.ssl.enable", dto.getSmtpSslEnabled());
            smtpProps.put("mail.smtp.connectiontimeout", "5000");
            smtpProps.put("mail.smtp.timeout", "5000");

            Session smtpSession = Session.getInstance(smtpProps);
            Transport transport = smtpSession.getTransport("smtp");
            transport.connect(dto.getEmailAddress(), dto.getPassword());
            transport.close();
        }catch (Exception e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SMTP Configuration Error: " + e.getMessage());
        }

        EmailAddress account = new EmailAddress();
        account.setUserId(userId);
        account.setEmailAddress(dto.getEmailAddress());
        account.setEncryptedPassword(emailCryptoService.encrypt(dto.getPassword()));
        account.setImapHost(dto.getImapHost());
        account.setImapPort(dto.getImapPort());
        account.setSslEnabled(dto.getSslEnabled() != null ? dto.getSslEnabled() : true);

        account.setSmtpHost(dto.getSmtpHost());
        account.setSmtpPort(dto.getSmtpPort());
        account.setSmtpSslEnabled(dto.getSmtpSslEnabled() != null ? dto.getSmtpSslEnabled() : true);

        return emailAddressRepository.save(account);
    }

    public List<EmailAddress> getAccountsByUserId(Long userId) {
        return emailAddressRepository.findByUserId(userId);
    }

    @Transactional
    public void deleteAccount(Long userId, String emailAddress) {
        EmailAddress account = emailAddressRepository.findByEmailAddressAndUserId(emailAddress, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Email account not found"));

        emailAddressRepository.delete(account);
    }

}
