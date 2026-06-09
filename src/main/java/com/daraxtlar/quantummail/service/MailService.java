package com.daraxtlar.quantummail.service;

import com.daraxtlar.quantummail.entity.ImapMail;
import com.daraxtlar.quantummail.model.Attachment;
import com.daraxtlar.quantummail.model.EmailMessage;
import com.daraxtlar.quantummail.repository.ImapMailRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


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

    @Autowired
    private ImapMailRepository imapMailRepository;

    private final ThreadLocal<Store> threadLocalStore = new ThreadLocal<>();

    private final Set<String> activeSyncs = ConcurrentHashMap.newKeySet();


    public void syncFolderFromImap(String accountEmail ,String folderName) {
        String folderToUse = (folderName == null || folderName.isEmpty()) ? "INBOX" : folderName;

        String syncKey = accountEmail + ":" + folderToUse;
        if (!activeSyncs.add(syncKey)) {
            System.out.println("[SYNC BLOCKED] Synchronizacja dla " + syncKey + " już trwa. Ignoruję duplikat żądania.");
            return;
        }

        Folder folder = null;

        try{
            getConnectedStore();
            folder = threadLocalStore.get().getFolder(folderToUse);
            folder.open(Folder.READ_ONLY);

            int totalMessages = folder.getMessageCount();
            if (totalMessages == 0) return;

            long localCount = imapMailRepository.countByAccountEmailAndFolderName(accountEmail, folderToUse.toUpperCase());

            int start = Math.max(1, (int) localCount - 15);

            if (start > totalMessages) {
                start = Math.max(1, totalMessages - 10);
            }

            System.out.println("[SYNC] Folder: " + folderToUse + " | Serwer: " + totalMessages + " | Baza: " + localCount + " | Sprawdzam od: " + start);

            Message[] messages = folder.getMessages(start, totalMessages);
            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);
            fp.add(UIDFolder.FetchProfileItem.UID);
            fp.add(FetchProfile.Item.CONTENT_INFO);
            folder.fetch(messages, fp);

            for (int i = messages.length - 1; i >= 0; i--) {
                Message message = messages[i];

                String uid;
                if (folder instanceof UIDFolder uidFolder) {
                    uid = String.valueOf(uidFolder.getUID(message));
                } else {
                    uid = String.valueOf(message.getMessageNumber());
                }

                boolean exists = imapMailRepository.existsByAccountEmailAndFolderNameAndUid(
                        accountEmail, folderToUse.toUpperCase(), uid);

                if (!exists) {
                    ImapMail imapMail = new ImapMail();
                    imapMail.setUid(uid);
                    imapMail.setAccountEmail(accountEmail);
                    imapMail.setFolderName(folderToUse.toUpperCase());
                    imapMail.setSubject(message.getSubject());
                    imapMail.setSentDate(message.getSentDate() != null ? message.getSentDate() : new java.util.Date());

                    if (message.getFrom() != null && message.getFrom().length > 0) {
                        imapMail.setSender(((InternetAddress) message.getFrom()[0]).getAddress());
                    } else {
                        imapMail.setSender("Unknown@domain.com");
                    }

                    imapMail.setSnippet(extractSnippet(message));
                    imapMail.setRead(message.isSet(Flags.Flag.SEEN));
                    imapMail.setStarred(message.isSet(Flags.Flag.FLAGGED));

                    imapMailRepository.save(imapMail);
                }
            }
        }catch (Exception e){
            System.err.println("[IMAP SYNC ERROR] Błąd synchronizacji folderu " + folderName);
            e.printStackTrace();
        } finally {
            activeSyncs.remove(syncKey);
            closeQuietly(folder);
        }
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

    public EmailMessage getEmailMessage(String accountEmail ,String folderName, long uid) {
        String folderToUse = (folderName == null || folderName.isEmpty()) ? "INBOX" : folderName;
        Folder folder = null;

        try {
            getConnectedStore();
            folder = threadLocalStore.get().getFolder(folderToUse);
            folder.open(Folder.READ_WRITE);

            if (folder instanceof UIDFolder uidFolder) {
                Message message = uidFolder.getMessageByUID(uid);

                if (message != null) {
                    if (!message.isSet(Flags.Flag.SEEN)) {
                        message.setFlag(Flags.Flag.SEEN, true);
                    }

                    try {
                        imapMailRepository.findByAccountEmailAndFolderNameAndUid(
                                accountEmail,
                                folderToUse.toUpperCase(),
                                String.valueOf(uid))
                                .ifPresent(localMail -> {
                                    if (!localMail.isRead()){
                                        localMail.setRead(true);
                                        imapMailRepository.save(localMail);
                                        System.out.println("[DB UPDATE] Mail UID " + uid + " oznaczony jako przeczytany w bazie.");
                                    }
                                });
                    }catch (Exception dbEx){
                        System.err.println("[DB UPDATE ERROR] Nie udało się zaktualizować statusu w bazie danych: " + dbEx.getMessage());
                    }

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

    public org.springframework.data.domain.Page<ImapMail> getEmailsFromDb(String accountEmail ,String folderName, String query, int page, int size) {
        String folderToUse = (folderName == null || folderName.isEmpty()) ? "INBOX" : folderName.toUpperCase();
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);

        if (query != null && !query.trim().isEmpty()) {
            return imapMailRepository.searchMails(accountEmail, folderToUse, query, pageable);
        }

        if ("STARRED".equals(folderToUse)) {
            return imapMailRepository.findByAccountEmailAndIsStarredTrueOrderBySentDateDescIdDesc(accountEmail, pageable);
        }

        return imapMailRepository.findByAccountEmailAndFolderNameOrderBySentDateDescIdDesc(accountEmail, folderToUse, pageable);
    }

}
