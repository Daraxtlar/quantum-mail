package com.daraxtlar.quantummail.service;

import com.daraxtlar.quantummail.entity.EmailAddress;
import com.daraxtlar.quantummail.model.EmailAddressDTO;
import com.daraxtlar.quantummail.repository.EmailAddressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}
