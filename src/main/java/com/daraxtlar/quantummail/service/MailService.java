package com.daraxtlar.quantummail.service;

import com.daraxtlar.quantummail.model.EmailMessage;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

@Service
public class MailService {
    @Value("${email.account.username}")
    private String username;

    @Value("${email.account.password}")
    private String password;

    @Value("${email.account.imap.host}")
    private String imapHost;

    @Value("${email.account.imap.port}")
    private int imapPort;

    @Value("${email.account.imap.ssl}")
    private boolean imapSsl;

    public List<EmailMessage> fetchEmails() {
        List<EmailMessage> emails = new ArrayList<>();

        try {
            Properties props = new Properties();
            props.put("mail.store.protocol", "imaps");
            props.put("mail.imaps.host", imapHost);
            props.put("mail.imaps.port", String.valueOf(imapPort));
            props.put("mail.imaps.ssl.enable", "true");
            props.put("mail.imaps.starttls.enable", "false");

            Session session = Session.getInstance(props);
            Store store = session.getStore("imaps");
            store.connect(imapHost, imapPort, username, password);

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            Message[] messages = inbox.getMessages();
            for (Message message : messages) {
                EmailMessage emailMsg = new EmailMessage();

                emailMsg.setId(UUID.randomUUID().toString());

                if (message.getFrom() != null && message.getFrom().length > 0) {
                    emailMsg.setFrom(((InternetAddress) message.getFrom()[0]).getAddress());
                }

                emailMsg.setSubject(message.getSubject());

                try{
                    emailMsg.setContent(message.getContent().toString());
                }catch (Exception e){
                    emailMsg.setContent("Unable to display content");
                }

                emailMsg.setSentDate(message.getSentDate());
                emailMsg.setRead(message.isSet(Flags.Flag.SEEN));

                List<String> attachmentsNames = getAttachmentsNames(message);
                emailMsg.setHasAttachments(!attachmentsNames.isEmpty());
                emailMsg.setAttachments(attachmentsNames);

                emails.add(emailMsg);
            }

            inbox.close(false);
            store.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return emails;
    }

    private List<String> getAttachmentsNames(Message message) {
        List<String> attachments = new ArrayList<>();

        try {
            Object content = message.getContent();
            if (content instanceof Multipart) {
                Multipart multipart = (Multipart) content;
                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart part = multipart.getBodyPart(i);
                    String disposition = part.getDisposition();

                    if (disposition != null && (disposition.equals(Part.ATTACHMENT) ||
                            disposition.equals(Part.INLINE))) {
                        attachments.add(part.getFileName());
                    }
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return attachments;
    }
}
