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

    private Store store;


    public List<EmailMessage> fetchEmails(String folderName, int page, int size) {
        String folderToUse = (folderName == null || folderName.isEmpty()) ? "INBOX" : folderName;
        List<EmailMessage> emails = new ArrayList<>();
        Folder folder = null;

        try{
            getConnectedStore();
            folder = store.getFolder(folderToUse);
            folder.open(Folder.READ_ONLY);

            int totalMessages = folder.getMessageCount();
            int end = totalMessages - (page * size);
            int start = Math.max(1, end-size + 1);

            if (end <= 0) return emails;

            Message[] messages = folder.getMessages(start, end);
            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);
            fp.add(UIDFolder.FetchProfileItem.UID);
            folder.fetch(messages, fp);

            for (int i = messages.length - 1; i >= 0; i--) {
                Message message = messages[i];
                emails.add(mapMessageToEmailMessage(message, folder));
            }
        }catch (Exception e){
            e.printStackTrace();
        } finally {
            closeQuietly(folder);
        }
        return emails;
    }

    private EmailMessage mapMessageToEmailMessage(Message message, Folder folder) throws Exception {
        EmailMessage emailMsg = new EmailMessage();

        if (folder instanceof UIDFolder uidFolder) {
            emailMsg.setId(String.valueOf(uidFolder.getUID(message)));
        }else {
            emailMsg.setId(String.valueOf(message.getMessageNumber()));
        }

        emailMsg.setSubject(message.getSubject());
        emailMsg.setSentDate(message.getSentDate());
        if (message.getFrom() != null && message.getFrom().length > 0) {
            emailMsg.setFrom(((InternetAddress) message.getFrom()[0]).getAddress());
        }

        emailMsg.setHasAttachments(message.isMimeType("multipart/mixed") || message.isMimeType("multipart/related"));
        emailMsg.setSnippet(extractSnippet(message));

        emailMsg.setContent("");
        return emailMsg;
    }

    public int getFolderMessageCount(String folderName) {
        String folderToUse = (folderName == null || folderName.isEmpty()) ? "INBOX" : folderName;
        Folder folder = null;

        try{
            getConnectedStore();
            folder = store.getFolder(folderToUse);
            folder.open(Folder.READ_ONLY);
            return folder.getMessageCount();
        } catch (Exception e) {
            return 0;
        } finally {
            closeQuietly(folder);
        }
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

    private synchronized void getConnectedStore() throws MessagingException {
        if (store != null && store.isConnected()) {
            return;
        }

        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", imapHost);
        props.put("mail.imaps.port", String.valueOf(imapPort));
        props.put("mail.imaps.ssl.enable", "true");
        props.put("mail.imaps.peek", "true");

        Session session = Session.getInstance(props);
        this.store = session.getStore("imaps");
        this.store.connect(imapHost, imapPort, username, password);
    }

    private void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try { resource.close(); } catch (Exception ignored) {}
        }
    }

    public EmailMessage getEmailMessage(String folderName, long uid) {
        String folderToUse = (folderName == null || folderName.isEmpty()) ? "INBOX" : folderName;
        Folder folder = null;

        try {
            getConnectedStore();
            folder = store.getFolder(folderToUse);
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


    public byte[] downloadAttachment(String folderName, long uid, String fileName) {
        String folderToUse = (folderName == null || folderName.isEmpty()) ? "INBOX" : folderName;
        Folder folder = null;

        try {
            getConnectedStore();
            folder = store.getFolder(folderToUse);
            folder.open(Folder.READ_ONLY);
            Message message = ((UIDFolder) folder).getMessageByUID(uid);

            return findPartDataRecursive(message, fileName);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeQuietly(folder);
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
}
