package com.daraxtlar.quantummail.service;

import com.daraxtlar.quantummail.model.Attachment;
import com.daraxtlar.quantummail.model.EmailMessage;
import com.daraxtlar.quantummail.repository.MailRepository;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.util.ByteArrayDataSource;
import org.jsoup.Jsoup;
import org.simplejavamail.api.email.AttachmentResource;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.converter.EmailConverter;
import org.simplejavamail.email.EmailBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import jakarta.mail.UIDFolder;

import java.awt.print.Pageable;
import java.time.LocalDateTime;
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

    @Autowired
    private MailRepository mailRepository;

    private final ThreadLocal<Store> threadLocalStore = new ThreadLocal<>();


    public List<EmailMessage> fetchEmails(String folderName, int page, int size) {
        String folderToUse = (folderName == null || folderName.isEmpty()) ? "INBOX" : folderName;
        List<EmailMessage> emails = new ArrayList<>();
        Folder folder = null;

        try{
            getConnectedStore();
            folder = threadLocalStore.get().getFolder(folderToUse);
            folder.open(Folder.READ_ONLY);

            int totalMessages = folder.getMessageCount();
            int end = totalMessages - (page * size);
            int start = Math.max(1, end-size + 1);

            if (end <= 0) return emails;

            Message[] messages = folder.getMessages(start, end);
            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);
            fp.add(UIDFolder.FetchProfileItem.UID);
            fp.add(FetchProfile.Item.CONTENT_INFO);
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
            folder = threadLocalStore.get().getFolder(folderToUse);
            folder.open(Folder.READ_ONLY);
            return folder.getMessageCount();
        } catch (Exception e) {
            return 0;
        } finally {
            closeQuietly(folder);
        }
    }

    private String extractSnippet(Message message) {
        try {
            String rawText = "";
            if (message.isMimeType("text/plain")) {
                rawText = message.getContent().toString();
            } else if (message.isMimeType("text/html")) {
                rawText = cleanHTML(message.getContent().toString());
            } else if (message.isMimeType("multipart/*")) {
                Multipart mp = (Multipart) message.getContent();
                for (int i = 0; i < mp.getCount(); i++) {
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

        } catch (Exception e) {
            return "Nie udało się pobrać podglądu";
        }
    }

    private String cleanHTML(String html) {
        if (html == null || html.isEmpty()){
            return "";
        }
        return Jsoup.parse(html).text();
    }

    private void getConnectedStore() throws MessagingException {
        Store store = threadLocalStore.get();
        if (store != null && store.isConnected()) return;

        Properties props = new Properties();
        props.put("mail.imaps.host", imapHost);
        props.put("mail.imaps.port", String.valueOf(imapPort));
        props.put("mail.imaps.ssl.enable", "true");
        props.put("mail.imaps.peek", "true");

        Session session = Session.getInstance(props);
        store = session.getStore("imaps");
        store.connect(imapHost, imapPort, username, password);
        threadLocalStore.set(store);
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
            folder = threadLocalStore.get().getFolder(folderToUse);
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
            folder = threadLocalStore.get().getFolder(folderToUse);
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

    public Email prepareBaseEmailFromImap(String folderName, long uid) {
        String folderToUse = (folderName == null || folderName.isEmpty()) ? "INBOX" : folderName;
        Folder folder = null;
        try {
            getConnectedStore();
            folder = threadLocalStore.get().getFolder(folderToUse);
            folder.open(Folder.READ_ONLY);

            if (folder instanceof UIDFolder uidFolder) {
                Message message = uidFolder.getMessageByUID(uid);
                if (message instanceof jakarta.mail.internet.MimeMessage mimeMessage) {
                    Email converted = EmailConverter.mimeMessageToEmail(mimeMessage);

                    List<AttachmentResource> fixedImages = new ArrayList<>();
                    for (AttachmentResource img : converted.getEmbeddedImages()) {
                        try {
                            byte[] data = img.getDataSource().getInputStream().readAllBytes();
                            if (data.length == 0) {
                                byte[] fetched = findPartDataRecursive(mimeMessage, img.getName());
                                if (fetched != null && fetched.length > 0) {
                                    String mime = img.getDataSource().getContentType();
                                    fixedImages.add(new AttachmentResource(img.getName(),
                                            new ByteArrayDataSource(fetched, mime)));
                                }
                            } else {
                                fixedImages.add(img);
                            }
                        } catch (Exception e) {
                            byte[] fetched = findPartDataRecursive(mimeMessage, img.getName());
                            if (fetched != null && fetched.length > 0) {
                                String mime = img.getDataSource().getContentType();
                                fixedImages.add(new AttachmentResource(img.getName(),
                                        new ByteArrayDataSource(fetched, mime)));
                            }
                        }
                    }

                    return EmailBuilder.copying(converted)
                            .clearEmbeddedImages()
                            .withEmbeddedImages(fixedImages)
                            .buildEmail();
                }
            }
        } catch (Exception e) {
            System.out.println("Błąd podczas przygotowania emaila z IMAP:");
            e.printStackTrace();
        } finally {
            closeQuietly(folder);
        }
        return null;
    }

    public List<String> getSuggestedRecipients(String senderEmail) {
        return mailRepository.findRecentRecipients(senderEmail);
    }

    public List<String> getGlobalSuggestedRecipients() {
        return mailRepository.findGlobalRecentRecipients(PageRequest.of(0, 50));
    }

}
