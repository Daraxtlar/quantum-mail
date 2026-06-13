package com.daraxtlar.quantummail.service;

import com.daraxtlar.quantummail.entity.EmailAddress;
import com.daraxtlar.quantummail.entity.ImapMail;
import com.daraxtlar.quantummail.entity.Mail;
import com.daraxtlar.quantummail.entity.User;
import com.daraxtlar.quantummail.model.Attachment;
import com.daraxtlar.quantummail.model.EmailMessage;
import com.daraxtlar.quantummail.repository.EmailAddressRepository;
import com.daraxtlar.quantummail.repository.ImapMailRepository;
import com.daraxtlar.quantummail.repository.MailRepository;
import com.daraxtlar.quantummail.repository.UserRepository;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.transaction.Transactional;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.simplejavamail.api.email.AttachmentResource;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.converter.EmailConverter;
import org.simplejavamail.email.EmailBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class MailService {
    @Autowired
    private MailRepository mailRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ImapMailRepository imapMailRepository;

    @Autowired
    private EmailAddressRepository emailAddressRepository;

    @Autowired
    private EmailCryptoService emailCryptoService;

    private final ThreadLocal<Map<String, String>> threadLocalFolderMap = new ThreadLocal<>();

    private final ThreadLocal<Store> threadLocalStore = new ThreadLocal<>();
    private final Set<String> activeSyncs = ConcurrentHashMap.newKeySet();

    private void getConnectedStore(String accountEmail, Long userId) throws MessagingException {
        Store store = threadLocalStore.get();

        if (store != null && store.isConnected() && store.getURLName().getUsername().equalsIgnoreCase(accountEmail)) {
            return;
        }

        if (store != null) {
            closeQuietly(store);
        }

        threadLocalFolderMap.remove();

        EmailAddress account = emailAddressRepository.findByEmailAddressAndUserId(accountEmail, userId)
                .orElseThrow(() -> new SecurityException("Konto email nie znalezione: " + accountEmail));

        Properties props = new Properties();
        props.put("mail.imaps.host", account.getImapHost());
        props.put("mail.imaps.port", String.valueOf(account.getImapPort()));
        props.put("mail.imaps.ssl.enable", String.valueOf(account.getSslEnabled()));
        props.put("mail.imaps.peek", "true");

        Session session = Session.getInstance(props);
        store = session.getStore("imaps");
        store.connect(account.getImapHost(), account.getImapPort(), account.getEmailAddress(), emailCryptoService.decrypt(account.getEncryptedPassword()));
        threadLocalStore.set(store);
    }



    public boolean syncFolderFromImap(Long userId, String accountEmail ,String folderName) {
        String folderToUse = (folderName == null || folderName.isEmpty()) ? "INBOX" : folderName;

        String syncKey = accountEmail + ":" + folderToUse;
        if (!activeSyncs.add(syncKey)) {
            System.out.println("[SYNC BLOCKED] Synchronizacja dla " + syncKey + " już trwa. Ignoruję duplikat żądania.");
            return false;
        }

        Folder folder = null;

        try{
            getConnectedStore(accountEmail, userId);
            folder = threadLocalStore.get().getFolder(folderToUse);

            if ("STARRED".equalsIgnoreCase(folderToUse)) {
                boolean isGmail = false;
                try {
                    if (threadLocalStore.get().getFolder("[Gmail]").exists()) {
                        isGmail = true;
                    }
                } catch (Exception ignored) {}

                if (!isGmail) {
                    System.out.println("[SYNC] Pomijam fizyczną synchronizację folderu STARRED dla konta nie-Gmail (gwiazdki aktualizują się automatycznie z INBOX/Wysłane).");
                    return true;
                }
            }

            String realFolderName = getRealFolderName(threadLocalStore.get(), folderToUse);
            folder = threadLocalStore.get().getFolder(realFolderName);

            if (folder == null || !folder.exists()) {
                System.err.println("[SYNC ERROR] Folder " + folderToUse + " (zmapowany na: " + realFolderName + ") nie istnieje na serwerze dla konta " + accountEmail);
                return false;
            }

            folder.open(Folder.READ_ONLY);

            int totalMessages = folder.getMessageCount();
            if (totalMessages == 0) return true;

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
                        if (message.getFrom()[0] instanceof InternetAddress internetAddress) {
                            imapMail.setSender(internetAddress.getAddress());
                        }
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
            return false;
        } finally {
            activeSyncs.remove(syncKey);
            closeQuietly(folder);
        }
        return true;
    }

    private Map<String, String> detectFolders(Store store) {
        Map<String, String> folderMap = new HashMap<>();
        folderMap.put("INBOX", "INBOX");

        try {
            Folder[] allFolders = store.getDefaultFolder().list("*");
            for (Folder folder : allFolders) {
                String nameLower = folder.getName().toLowerCase();
                String fullName = folder.getFullName();

                try {
                    java.lang.reflect.Method getAttributesMethod = folder.getClass().getMethod("getAttributes");
                    String[] attributes = (String[]) getAttributesMethod.invoke(folder);

                    if (attributes != null) {
                        for (String attr : attributes) {
                            if (attr.equalsIgnoreCase("\\Sent")) folderMap.put("SENT", fullName);
                            else if (attr.equalsIgnoreCase("\\Trash")) folderMap.put("TRASH", fullName);
                            else if (attr.equalsIgnoreCase("\\Junk") || attr.equalsIgnoreCase("\\Spam")) folderMap.put("SPAM", fullName);
                            else if (attr.equalsIgnoreCase("\\Drafts")) folderMap.put("DRAFTS", fullName);
                        }
                    }
                } catch (Exception ignored) {
                }

                if (!folderMap.containsKey("SPAM") && (nameLower.contains("spam") || nameLower.contains("niechciane") || nameLower.contains("junk") || nameLower.contains("śmieci") || nameLower.contains("smieci"))) {
                    folderMap.put("SPAM", fullName);
                }
                if (!folderMap.containsKey("SENT") && (nameLower.contains("sent") || nameLower.contains("wysłane") || nameLower.contains("wyslane"))) {
                    folderMap.put("SENT", fullName);
                }
                if (!folderMap.containsKey("TRASH") && (nameLower.contains("trash") || nameLower.contains("kosz") || nameLower.contains("usunięte") || nameLower.contains("usuniete"))) {
                    folderMap.put("TRASH", fullName);
                }
                if (!folderMap.containsKey("DRAFTS") && (nameLower.contains("draft") || nameLower.contains("szablony") || nameLower.contains("robocze"))) {
                    folderMap.put("DRAFTS", fullName);
                }
            }

            try {
                if (store.getFolder("[Gmail]").exists()) {
                    folderMap.put("STARRED", "[Gmail]/Starred");
                }
            } catch (Exception ignored) {}

        } catch (Exception e) {
            System.err.println("[IMAP DETECT ERROR] Nie udało się automatycznie zmapować folderów: " + e.getMessage());
        }
        return folderMap;
    }

    private String getRealFolderName(Store store, String folderName) {
        String cleaned = (folderName == null || folderName.isEmpty()) ? "INBOX" : folderName.trim();
        String upper = cleaned.toUpperCase();

        if ("INBOX".equals(upper)) return "INBOX";

        Map<String, String> cachedMap = threadLocalFolderMap.get();
        if (cachedMap == null) {
            System.out.println("[IMAP] Pierwsze zapytanie w sesji - skanuję strukturę folderów serwera...");
            cachedMap = detectFolders(store);
            threadLocalFolderMap.set(cachedMap);
        }

        return cachedMap.getOrDefault(upper, cleaned);
    }

    public int getFolderMessageCount(Long userId, String accountEmail, String folderName) {
        Folder folder = null;

        try{
            getConnectedStore(accountEmail, userId);
            String realFolderName = getRealFolderName(threadLocalStore.get(), folderName);
            folder = threadLocalStore.get().getFolder(realFolderName);
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


    private void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try { resource.close(); } catch (Exception ignored) {}
        }
    }

    public EmailMessage getEmailMessage(Long userId, String accountEmail ,String folderName, long uid) {
        Folder folder = null;

        try {
            getConnectedStore(accountEmail, userId);
            String realFolderName = getRealFolderName(threadLocalStore.get(), folderName);
            folder = threadLocalStore.get().getFolder(realFolderName);
            folder.open(Folder.READ_WRITE);

            if (folder instanceof UIDFolder uidFolder) {
                Message message = uidFolder.getMessageByUID(uid);

                if (message != null) {
                    if (!message.isSet(Flags.Flag.SEEN)) {
                        message.setFlag(Flags.Flag.SEEN, true);
                    }

                    String uniformFolder = (folderName == null || folderName.isEmpty()) ? "INBOX" : folderName.toUpperCase();
                    try {
                        imapMailRepository.findByAccountEmailAndFolderNameAndUid(
                                accountEmail,
                                uniformFolder,
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


    public byte[] downloadAttachment(Long userId,String accountEmail,String folderName, long uid, String fileName) {
        Folder folder = null;

        try {
            getConnectedStore(accountEmail, userId);
            String realFolderName = getRealFolderName(threadLocalStore.get(), folderName);
            folder = threadLocalStore.get().getFolder(realFolderName);
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

    public Email prepareBaseEmailFromImap(Long userId, String accountEmail, String folderName, long uid) {
        Folder folder = null;
        try {
            getConnectedStore(accountEmail, userId);
            String realFolderName = getRealFolderName(threadLocalStore.get(), folderName);
            folder = threadLocalStore.get().getFolder(realFolderName);
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

    public List<String> getSuggestedRecipients(Long userId, String senderEmail) {
        return mailRepository.findRecentRecipientsByEmail(userId, senderEmail, PageRequest.of(0, 10));
    }

    public List<String> getGlobalSuggestedRecipients(Long userId) {
        return mailRepository.findRecentRecipientsByAccount(userId, PageRequest.of(0, 20));
    }

    @Transactional
    public Mail addSuggestedRecipient(Long userId, String senderEmail, String recipientEmail) {
        Optional<Mail> existingMail = mailRepository.findBySenderEmailAndRecipientEmailAndUserId(senderEmail, recipientEmail, userId);

        if (existingMail.isPresent()) {
            Mail mail = existingMail.get();
            mail.setSentDate(LocalDateTime.now());
            return mailRepository.save(mail);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Mail newMail = new Mail();
        newMail.setUser(user);
        newMail.setSenderEmail(senderEmail);
        newMail.setRecipientEmail(recipientEmail);
        newMail.setSentDate(LocalDateTime.now());

        return mailRepository.save(newMail);
    }

    @Transactional
    public void deleteSuggestedRecipient(Long userId, String senderEmail, String recipientEmail) {
        Mail mail = mailRepository.findBySenderEmailAndRecipientEmailAndUserId(senderEmail, recipientEmail, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Suggested recipient not found"));

        mailRepository.delete(mail);
    }

    public List<String> getUserEmailAccounts(Long userId) {
        return emailAddressRepository.findByUserId(userId)
                .stream()
                .map(EmailAddress::getEmailAddress)
                .toList();
    }



    public org.springframework.data.domain.Page<ImapMail> getEmailsFromDb(Long userId, String accountEmail ,String folderName, String query, int page, int size) {
        boolean hasAccess = emailAddressRepository.findByEmailAddressAndUserId(accountEmail, userId).isPresent();
        if (!hasAccess) {
            throw new SecurityException("Brak dostępu do tego konta email: " + accountEmail);
        }

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
