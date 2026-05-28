package com.daraxtlar.quantummail.service;

import com.daraxtlar.quantummail.model.Attachment;
import com.daraxtlar.quantummail.model.EmailMessage;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.mail.UIDFolder;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


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

    private List<EmailMessage> cachedEmails = new ArrayList<>();
    private long lastFetch = 0;
    private static final long CACHE_MS = 60_000;

    public List<EmailMessage> fetchEmails() {
        if (System.currentTimeMillis() - lastFetch < CACHE_MS && !cachedEmails.isEmpty()) {
            return cachedEmails;
        }

        List<EmailMessage> emails = new ArrayList<>();
        Store store = null;
        Folder inbox = null;

        try {
            store = getConnectedStore();
            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            Message[] messages = inbox.getMessages();

            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);
            fp.add(UIDFolder.FetchProfileItem.UID);
            fp.add(FetchProfile.Item.CONTENT_INFO);
            inbox.fetch(messages, fp);

            for (int i = messages.length - 1; i >= 0; i--) {
                Message message = messages[i];
                EmailMessage emailMsg = new EmailMessage();

                if (inbox instanceof UIDFolder uidFolder) {
                    emailMsg.setId(String.valueOf(uidFolder.getUID(message)));
                } else {
                    emailMsg.setId(String.valueOf(i));
                }

                emailMsg.setSubject(message.getSubject());
                emailMsg.setSentDate(message.getSentDate());

                if (message.getFrom() != null && message.getFrom().length > 0) {
                    emailMsg.setFrom(((InternetAddress) message.getFrom()[0]).getAddress());
                }

                emailMsg.setHasAttachments(message.isMimeType("multipart/mixed") || message.isMimeType("multipart/related"));
                emailMsg.setSnippet(extractSnippet(message));

                emailMsg.setContent("");

                emails.add(emailMsg);
            }

            cachedEmails = emails;
            lastFetch = System.currentTimeMillis();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeQuietly(inbox);
            closeQuietly(store);
        }

        return emails;
    }

    private String extractSnippet(Message message) {
        try{
            String rawText = "";
            if (message.isMimeType("text/plain")) {
                rawText = message.getContent().toString();
            } else if (message.isMimeType("text/html")) {
                rawText = cleanHTML(message.getContent().toString());
            } else if (message.isMimeType("multipart/*")) {
                Multipart mp = (Multipart) message.getContent();
                for (int i = 0; i < mp.getCount(); i++){
                    BodyPart part = mp.getBodyPart(i);
                    if (part.isMimeType("text/plain")) {
                        rawText = part.getContent().toString();
                        break;
                    } else if (part.isMimeType("text/html")) {
                        rawText = cleanHTML(part.getContent().toString());
                        break;
                    }
                }
            }

            String snippet = rawText.replaceAll("\\s+", " ").trim();

            return snippet.length() > 120 ? snippet.substring(0, 117) + "..." : snippet;

        }catch (Exception e) {
            return "Nie udało się pobrać podglądu";
        }
    }

    private String cleanHTML(String html) {
        if (html == null || html.isEmpty()){
            return "";
        }
        return Jsoup.parse(html).text();
    }

    private Store getConnectedStore() throws MessagingException {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", imapHost);
        props.put("mail.imaps.port", String.valueOf(imapPort));
        props.put("mail.imaps.ssl.enable", "true");
        props.put("mail.imaps.peek", "true");

        Session session = Session.getInstance(props);
        Store store = session.getStore("imaps");
        store.connect(imapHost, imapPort, username, password);
        return store;
    }

    private void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try { resource.close(); } catch (Exception ignored) {}
        }
    }

    public EmailMessage getEmailMessage(long uid) {
        Store store = null;
        Folder folder = null;

        try {
            store = getConnectedStore();
            folder = store.getFolder("INBOX");
            folder.open(Folder.READ_ONLY);

            if (folder instanceof UIDFolder uidFolder) {
                Message message = uidFolder.getMessageByUID(uid);

                if (message != null) {
                    EmailMessage emailMsg = new EmailMessage();
                    emailMsg.setId(String.valueOf(uid));
                    emailMsg.setSubject(message.getSubject());
                    emailMsg.setSentDate(message.getSentDate());

                    if (message.getFrom() != null && message.getFrom().length > 0) {
                        emailMsg.setFrom(((InternetAddress) message.getFrom()[0]).getAddress());
                    }
                    emailMsg.setContent(getFullContent(message));

                    emailMsg.setAttachments(getAttachmentsInfo(message));
                    emailMsg.setHasAttachments(!emailMsg.getAttachments().isEmpty());
                    return emailMsg;
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            closeQuietly(folder);
            closeQuietly(store);
        }

        return null;
    }

    private String getFullContent(Part part) throws Exception {
        if (part.isMimeType("text/html")) {
            return (String) part.getContent();
        }else if (part.isMimeType("text/plain")) {
            return (String) part.getContent();
        }else if (part.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) part.getContent();
            String result = "";
            for (int i = 0; i < mp.getCount(); i++) {
                BodyPart bodyPart = mp.getBodyPart(i);
                String content = getFullContent(bodyPart);

                if (bodyPart.isMimeType("text/html")) {
                    return content;
                }
                result = content;
            }
            return result;
        }
        return "Nie można wyświetlić wiadomości";
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


    public byte[] downloadAttachment(long uid, String fileName) {
        Store store = null;
        Folder folder = null;
        try {
            store = getConnectedStore();
            folder = store.getFolder("INBOX");
            folder.open(Folder.READ_ONLY);
            Message message = ((UIDFolder) folder).getMessageByUID(uid);

            return findPartDataRecursive(message, fileName);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeQuietly(folder);
            closeQuietly(store);
        }
        return null;
    }

    private byte[] findPartDataRecursive(Part part, String fileName) throws Exception {
        if (fileName.equalsIgnoreCase(part.getFileName())) {
            return part.getInputStream().readAllBytes();
        }

        if (part instanceof BodyPart bp) {
            String[] cidHeaders = bp.getHeader("Content-ID");
            if (cidHeaders != null && cidHeaders.length > 0) {
                String cid = cidHeaders[0].replaceAll("[<>]", ""); // Czyścimy < >
                if (fileName.equalsIgnoreCase(cid)) {
                    return part.getInputStream().readAllBytes();
                }
            }
        }

        if (part.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) part.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                byte[] data = findPartDataRecursive(mp.getBodyPart(i), fileName);
                if (data != null) return data;
            }
        }
        return null;
    }

    private List<Attachment> getAttachmentsInfo(Message message) throws Exception {
        List<Attachment> attachments = new ArrayList<>();
        extractAttachmentsRecursive(message, attachments);
        return attachments;
    }

    private void extractAttachmentsRecursive(Part part, List<Attachment> list) throws Exception {
        String disposition = part.getDisposition();
        String fileName = part.getFileName();
        String contentType = part.getContentType().toLowerCase();

        boolean isInlineImage = (contentType.startsWith("image/") &&
                (Part.INLINE.equalsIgnoreCase(disposition) || disposition == null));

        if (fileName != null && !isInlineImage) {
            list.add(new Attachment(fileName, part.getContentType(), part.getSize()));
        }
        
        if (part.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) part.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                extractAttachmentsRecursive(mp.getBodyPart(i), list);
            }
        }
    }

    public void clearCache() {
        cachedEmails.clear();
        lastFetch = 0;
    }
}
