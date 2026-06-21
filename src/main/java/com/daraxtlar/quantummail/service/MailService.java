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
import org.jsoup.Jsoup;
import org.simplejavamail.api.email.AttachmentResource;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.converter.EmailConverter;
import org.simplejavamail.email.EmailBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service responsible for email synchronization, retrieval and mailbox
 * management operations.
 *
 * <p>Provides functionality for synchronizing messages from external IMAP
 * servers, managing mailbox folders, retrieving email content and
 * maintaining locally cached email metadata.</p>
 */
@Service
public class MailService {

    /**
     * Thread-local cache containing detected folder mappings
     * for the currently connected mailbox.
     */
    private final ThreadLocal<Map<String, String>> threadLocalFolderMap = new ThreadLocal<>();
    /**
     * Thread-local IMAP store connection used during mailbox operations.
     */
    private final ThreadLocal<Store> threadLocalStore = new ThreadLocal<>();
    /**
     * Collection of currently active mailbox synchronizations used to
     * prevent concurrent synchronization of the same folder.
     */
    private final Set<String> activeSyncs = ConcurrentHashMap.newKeySet();
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

    /**
     * Establishes or reuses an IMAP connection for the specified email account.
     *
     * <p>If an active connection for the requested account already exists in the
     * current thread, it is reused. Otherwise, a new authenticated IMAP
     * connection is created and stored in thread-local storage.</p>
     *
     * @param accountEmail email account address
     * @param userId       owner of the email account
     * @throws MessagingException if the IMAP connection cannot be established
     * @throws SecurityException  if the email account configuration cannot be found
     */
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

    /**
     * Synchronizes a mailbox folder with the remote IMAP server.
     *
     * <p>The method downloads message metadata, updates message status flags
     * such as read and starred state, and stores new messages in the local
     * database. Duplicate synchronization requests for the same mailbox folder
     * are automatically ignored.</p>
     *
     * @param userId       identifier of the authenticated user
     * @param accountEmail email account address
     * @param folderName   mailbox folder to synchronize
     * @return {@code true} if synchronization completed successfully,
     * otherwise {@code false}
     */
    public boolean syncFolderFromImap(Long userId, String accountEmail, String folderName) {
        String folderToUse = (folderName == null || folderName.isEmpty()) ? "INBOX" : folderName;

        String syncKey = accountEmail + ":" + folderToUse;
        if (!activeSyncs.add(syncKey)) {
            System.out.println("[SYNC BLOCKED] Synchronizacja dla " + syncKey + " już trwa. Ignoruję duplikat żądania.");
            return false;
        }

        Folder folder = null;

        try {
            getConnectedStore(accountEmail, userId);
            Store store = threadLocalStore.get();

            if ("STARRED".equalsIgnoreCase(folderToUse)) {
                boolean isGmail = false;
                try {
                    if (store.getFolder("[Gmail]").exists()) {
                        isGmail = true;
                    }
                } catch (Exception ignored) {
                }

                if (!isGmail) {
                    System.out.println("[SYNC] Pomijam fizyczną synchronizację folderu STARRED dla konta nie-Gmail (gwiazdki aktualizują się automatycznie z INBOX/Wysłane).");
                    syncNonGmailStarred(store, accountEmail);
                    return true;
                }
            }

            String realFolderName = getRealFolderName(store, folderToUse);
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

            String uniformFolder = folderToUse.toUpperCase();

            for (int i = messages.length - 1; i >= 0; i--) {
                Message message = messages[i];

                String uid = (folder instanceof UIDFolder uidFolder)
                        ? String.valueOf(uidFolder.getUID(message))
                        : String.valueOf(message.getMessageNumber());

                boolean serverRead = message.isSet(Flags.Flag.SEEN);
                boolean serverStarred = message.isSet(Flags.Flag.FLAGGED);

                String senderEmail = "Unknown@domain.com";
                if (message.getFrom() != null && message.getFrom().length > 0) {
                    if (message.getFrom()[0] instanceof InternetAddress internetAddress) {
                        senderEmail = internetAddress.getAddress();
                    }
                }

                Optional<ImapMail> existingMailOpt = imapMailRepository.findByAccountEmailAndFolderNameAndUid(
                        accountEmail, uniformFolder, uid);

                if (existingMailOpt.isPresent()) {
                    ImapMail localMail = existingMailOpt.get();
                    boolean isChanged = false;

                    if (localMail.isRead() != serverRead) {
                        localMail.setRead(serverRead);
                        isChanged = true;
                    }

                    if (localMail.isStarred() != serverStarred) {
                        localMail.setStarred(serverStarred);
                        isChanged = true;
                    }

                    if (isChanged) {
                        imapMailRepository.save(localMail);
                    }
                } else {
                    Date sentDate = message.getSentDate() != null ? message.getSentDate() : new Date();

                    Optional<ImapMail> duplicateCheck = imapMailRepository.findFirstByAccountEmailAndSenderAndSubjectAndSentDate(accountEmail, senderEmail, message.getSubject(), sentDate);
                    if (duplicateCheck.isPresent()) {
                        ImapMail existingCrossMail = duplicateCheck.get();
                        if (existingCrossMail.isStarred() != serverStarred || existingCrossMail.isRead() != serverRead) {
                            existingCrossMail.setStarred(serverStarred);
                            existingCrossMail.setRead(serverRead);
                            imapMailRepository.save(existingCrossMail);
                        }
                        continue;
                    }

                    ImapMail imapMail = new ImapMail();
                    imapMail.setUid(uid);
                    imapMail.setAccountEmail(accountEmail);
                    imapMail.setFolderName(uniformFolder);
                    imapMail.setSubject(message.getSubject());
                    imapMail.setSentDate(message.getSentDate() != null ? message.getSentDate() : new Date());

                    if (message.getFrom() != null && message.getFrom().length > 0) {
                        if (message.getFrom()[0] instanceof InternetAddress internetAddress) {
                            imapMail.setSender(internetAddress.getAddress());
                        }
                    } else {
                        imapMail.setSender("Unknown@domain.com");
                    }

                    imapMail.setSnippet(extractSnippet(message));
                    imapMail.setRead(serverRead);
                    imapMail.setStarred(serverStarred);

                    imapMailRepository.save(imapMail);
                }
            }
        } catch (Exception e) {
            System.err.println("[IMAP SYNC ERROR] Błąd synchronizacji folderu " + folderName);
            e.printStackTrace();
            return false;
        } finally {
            activeSyncs.remove(syncKey);
            closeQuietly(folder);
        }
        return true;
    }

    /**
     * Synchronizes starred messages for non-Gmail accounts.
     *
     * <p>Since many mail providers do not expose a dedicated starred folder,
     * flagged messages are retrieved from the inbox and synchronized with
     * locally stored message records.</p>
     *
     * @param store        active IMAP store connection
     * @param accountEmail synchronized email account
     * @throws Exception if synchronization fails
     */
    private void syncNonGmailStarred(Store store, String accountEmail) throws Exception {
        Folder inbox = store.getFolder("INBOX");

        if (inbox == null || !inbox.exists()) return;
        inbox.open(Folder.READ_ONLY);

        Message[] messages = inbox.search(new jakarta.mail.search.FlagTerm(new Flags(Flags.Flag.FLAGGED), true));

        if (messages.length == 0) {
            inbox.close(false);
            return;
        }

        FetchProfile fp = new FetchProfile();
        fp.add(FetchProfile.Item.ENVELOPE);
        fp.add(UIDFolder.FetchProfileItem.UID);
        fp.add(FetchProfile.Item.CONTENT_INFO);
        inbox.fetch(messages, fp);

        for (Message message : messages) {
            String uid = (inbox instanceof UIDFolder uidFolder)
                    ? String.valueOf(uidFolder.getUID(message))
                    : String.valueOf(message.getMessageNumber());

            boolean serverRead = message.isSet(Flags.Flag.SEEN);

            Optional<ImapMail> existingMailOpt = imapMailRepository.findByAccountEmailAndFolderNameAndUid(
                    accountEmail, "INBOX", uid);

            if (existingMailOpt.isPresent()) {
                ImapMail localMail = existingMailOpt.get();
                if (!localMail.isStarred() || localMail.isRead() != serverRead) {
                    localMail.setStarred(true);
                    localMail.setRead(serverRead);
                    imapMailRepository.save(localMail);
                }
            } else {
                ImapMail imapMail = new ImapMail();
                imapMail.setUid(uid);
                imapMail.setAccountEmail(accountEmail);
                imapMail.setFolderName("INBOX");
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
                imapMail.setRead(serverRead);
                imapMail.setStarred(true);

                imapMailRepository.save(imapMail);
            }
        }
        inbox.close(false);
    }

    /**
     * Detects standard mailbox folders available on the remote mail server.
     *
     * <p>The method attempts to identify commonly used folders such as Inbox,
     * Sent, Trash, Spam, Drafts and Starred using both IMAP folder attributes
     * and localized folder names.</p>
     *
     * @param store active IMAP store connection
     * @return mapping between logical folder names and actual server folder names
     */
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
                            else if (attr.equalsIgnoreCase("\\Junk") || attr.equalsIgnoreCase("\\Spam"))
                                folderMap.put("SPAM", fullName);
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
            } catch (Exception ignored) {
            }

        } catch (Exception e) {
            System.err.println("[IMAP DETECT ERROR] Nie udało się automatycznie zmapować folderów: " + e.getMessage());
        }
        return folderMap;
    }

    /**
     * Resolves a logical folder name to the actual folder name used by the
     * remote mail server.
     *
     * <p>Folder mappings are detected once per thread and cached for
     * subsequent mailbox operations.</p>
     *
     * @param store      active IMAP store connection
     * @param folderName logical folder name
     * @return actual folder name on the mail server
     */
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

    /**
     * Extracts a short preview snippet from an email message.
     *
     * <p>The method supports plain text, HTML and multipart messages and
     * returns a shortened representation suitable for mailbox listings.</p>
     *
     * @param message source email message
     * @return message preview snippet
     */
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

    /**
     * Removes HTML tags and returns plain text content.
     *
     * @param html HTML content
     * @return cleaned plain text
     */
    private String cleanHTML(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        return Jsoup.parse(html).text();
    }

    /**
     * Safely closes a resource without propagating exceptions.
     *
     * @param resource resource to close
     */
    private void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Moves an email message between folders or updates its starred status.
     *
     * <p>For standard folder operations, the message is copied to the target
     * folder and removed from the source folder. For starred operations,
     * the IMAP flagged status is updated without physically moving the
     * message.</p>
     *
     * @param userId           authenticated user identifier
     * @param accountEmail     email account address
     * @param sourceFolderName source folder
     * @param targetFolderName destination folder
     * @param uid              message UID
     * @param sender           sender email address
     * @param subject          message subject
     * @param sentDate         message sent date
     * @return {@code true} if the operation succeeds, otherwise {@code false}
     */
    @Transactional
    public boolean moveEmailToFolder(Long userId, String accountEmail, String sourceFolderName, String targetFolderName,
                                     long uid, String sender, String subject, java.util.Date sentDate) {
        String uniformSourceFolder = (sourceFolderName == null || sourceFolderName.isEmpty()) ? "INBOX" : sourceFolderName.toUpperCase();
        String uniformTargetFolder = (targetFolderName == null) ? "" : targetFolderName.toUpperCase();

        Folder sourceFolder = null;
        boolean isSuccess = false;
        boolean shouldExpunge = false;

        boolean isStarredAction = "STARRED".equals(uniformTargetFolder);

        boolean isMovingFromStarredToInbox = "STARRED".equals(uniformSourceFolder) && "INBOX".equals(uniformTargetFolder);

        try {
            getConnectedStore(accountEmail, userId);
            Store store = threadLocalStore.get();

            Optional<ImapMail> localMailOpt;
            if ("STARRED".equals(uniformSourceFolder)) {
                localMailOpt = imapMailRepository.findFirstByAccountEmailAndSenderAndSubjectAndSentDate(
                        accountEmail, sender, subject, sentDate);
            } else {
                localMailOpt = imapMailRepository.findByAccountEmailAndFolderNameAndUid(
                        accountEmail, uniformSourceFolder, String.valueOf(uid));
            }

            if (localMailOpt.isEmpty()) {
                System.err.println("[MOVE ERROR] Nie znaleziono maila w bazie danych.");
                return false;
            }

            ImapMail localMail = localMailOpt.get();
            String realSourceFolderName = localMail.getFolderName();
            long realImapUid = Long.parseLong(localMail.getUid());

            String realSourceFolder = getRealFolderName(store, realSourceFolderName);
            sourceFolder = store.getFolder(realSourceFolder);

            if (sourceFolder == null || !sourceFolder.exists()) {
                System.err.println("[MOVE ERROR] Folder źródłowy: " + realSourceFolder + " nie istnieje na serwerze.");
                return false;
            }

            sourceFolder.open(Folder.READ_WRITE);

            if (sourceFolder instanceof UIDFolder uidFolder) {
                Message message = uidFolder.getMessageByUID(realImapUid);

                if (message == null) {
                    System.err.println("[MOVE ERROR] Nie znaleziono wiadomości o UID " + realImapUid + " w folderze " + realSourceFolder);
                    return false;
                }

                if (isStarredAction || isMovingFromStarredToInbox) {
                    boolean nextStarredState;

                    if (isMovingFromStarredToInbox) {
                        nextStarredState = false;
                    } else {
                        nextStarredState = !localMail.isStarred();
                    }

                    message.setFlag(Flags.Flag.FLAGGED, nextStarredState);
                    isSuccess = true;

                    localMail.setStarred(nextStarredState);
                    imapMailRepository.save(localMail);

                    System.out.println("[DB UPDATE] Przeniesiono/Zmieniono stan STARRED na: " + nextStarredState + " w folderze: " + realSourceFolderName);

                } else {
                    String realTargetFolder = getRealFolderName(store, uniformTargetFolder);
                    Folder targetFolder = store.getFolder(realTargetFolder);

                    if (targetFolder == null || !targetFolder.exists()) {
                        System.err.println("[MOVE ERROR] Folder docelowy: " + realTargetFolder + " nie istnieje.");
                        return false;
                    }

                    sourceFolder.copyMessages(new Message[]{message}, targetFolder);
                    message.setFlag(Flags.Flag.DELETED, true);

                    isSuccess = true;
                    shouldExpunge = true;

                    imapMailRepository.delete(localMail);
                    System.out.println("[DB UPDATE] Usunięto stary rekord maila z folderu: " + realSourceFolderName);
                }
            }
        } catch (Exception e) {
            System.err.println("[MOVE ERROR] Błąd podczas operacji na mailu.");
            e.printStackTrace();
            return false;
        } finally {
            if (sourceFolder != null && sourceFolder.isOpen()) {
                try {
                    sourceFolder.close(shouldExpunge);
                } catch (Exception ignored) {
                }
            }
        }
        return isSuccess;
    }

    /**
     * Retrieves full details of an email message from the IMAP server.
     *
     * <p>The method marks the message as read on both the server and the
     * local database and returns message content together with attachment
     * metadata.</p>
     *
     * @param userId       authenticated user identifier
     * @param accountEmail email account address
     * @param folderName   mailbox folder
     * @param uid          IMAP message UID
     * @return populated email message model or {@code null} if not found
     */
    public EmailMessage getEmailMessage(Long userId, String accountEmail, String folderName, long uid) {
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
                                    if (!localMail.isRead()) {
                                        localMail.setRead(true);
                                        imapMailRepository.save(localMail);
                                        System.out.println("[DB UPDATE] Mail UID " + uid + " oznaczony jako przeczytany w bazie.");
                                    }
                                });
                    } catch (Exception dbEx) {
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
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeQuietly(folder);
        }

        return null;
    }

    /**
     * Extracts the complete content of an email message.
     *
     * <p>HTML content is preferred when available. Multipart messages are
     * traversed recursively to locate the most appropriate message body.</p>
     *
     * @param part MIME part to process
     * @return extracted message content
     * @throws Exception if content extraction fails
     */
    private String getFullContent(Part part) throws Exception {
        if (part.isMimeType("text/html")) {
            return (String) part.getContent();
        } else if (part.isMimeType("text/plain")) {
            return (String) part.getContent();
        } else if (part.isMimeType("multipart/*")) {
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

    /**
     * Downloads an attachment from an email message.
     *
     * @param userId       authenticated user identifier
     * @param accountEmail email account address
     * @param folderName   mailbox folder
     * @param uid          message UID
     * @param fileName     attachment filename
     * @return attachment data as a byte array or {@code null} if not found
     */
    public byte[] downloadAttachment(Long userId, String accountEmail, String folderName, long uid, String fileName) {
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

    /**
     * Recursively searches a MIME structure for an attachment or embedded
     * resource and returns its binary data.
     *
     * @param part     MIME part to inspect
     * @param fileName target filename or content identifier
     * @return attachment data or {@code null} if not found
     * @throws Exception if MIME processing fails
     */
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

    /**
     * Collects metadata for all non-inline attachments contained in a message.
     *
     * @param message email message
     * @return list of attachment descriptors
     * @throws Exception if attachment processing fails
     */
    private List<Attachment> getAttachmentsInfo(Message message) throws Exception {
        List<Attachment> attachments = new ArrayList<>();
        extractAttachmentsRecursive(message, attachments);
        return attachments;
    }

    /**
     * Recursively traverses a MIME structure and extracts attachment metadata.
     *
     * @param part MIME part to inspect
     * @param list destination attachment collection
     * @throws Exception if attachment extraction fails
     */
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

    /**
     * Loads an email message from the IMAP server and converts it into a
     * reusable email representation.
     *
     * <p>This method is primarily used for reply and forward operations,
     * preserving embedded images and attachment references.</p>
     *
     * @param userId       authenticated user identifier
     * @param accountEmail email account address
     * @param folderName   mailbox folder
     * @param uid          message UID
     * @return prepared email object or {@code null} if conversion fails
     */
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

    /**
     * Retrieves recently used recipients for a specific sender account.
     *
     * @param userId      authenticated user identifier
     * @param senderEmail sender email address
     * @return list of suggested recipient addresses
     */
    public List<String> getSuggestedRecipients(Long userId, String senderEmail) {
        return mailRepository.findRecentRecipientsByEmail(userId, senderEmail, PageRequest.of(0, 10));
    }

    /**
     * Retrieves recently used recipients across all user email accounts.
     *
     * @param userId authenticated user identifier
     * @return list of suggested recipient addresses
     */
    public List<String> getGlobalSuggestedRecipients(Long userId) {
        return mailRepository.findRecentRecipientsByAccount(userId, PageRequest.of(0, 20));
    }

    /**
     * Adds or updates a recipient suggestion entry.
     *
     * <p>If the recipient already exists, its last usage timestamp is
     * refreshed instead of creating a duplicate record.</p>
     *
     * @param userId         authenticated user identifier
     * @param senderEmail    sender email address
     * @param recipientEmail recipient email address
     * @return persisted suggestion entity
     */
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

    /**
     * Removes a saved recipient suggestion.
     *
     * @param userId         authenticated user identifier
     * @param senderEmail    sender email address
     * @param recipientEmail recipient email address
     */
    @Transactional
    public void deleteSuggestedRecipient(Long userId, String senderEmail, String recipientEmail) {
        Mail mail = mailRepository.findBySenderEmailAndRecipientEmailAndUserId(senderEmail, recipientEmail, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Suggested recipient not found"));

        mailRepository.delete(mail);
    }

    /**
     * Retrieves all email accounts associated with a user.
     *
     * @param userId authenticated user identifier
     * @return list of email account addresses
     */
    public List<String> getUserEmailAccounts(Long userId) {
        return emailAddressRepository.findByUserId(userId)
                .stream()
                .map(EmailAddress::getEmailAddress)
                .toList();
    }

    /**
     * Retrieves paginated email metadata from the local database.
     *
     * <p>The method supports folder filtering, full-text search and special
     * handling of starred messages.</p>
     *
     * @param userId       authenticated user identifier
     * @param accountEmail email account address
     * @param folderName   mailbox folder
     * @param query        optional search phrase
     * @param page         requested page number
     * @param size         page size
     * @return page containing email metadata records
     * @throws SecurityException if the user does not have access to the
     *                           specified email account
     */
    public org.springframework.data.domain.Page<ImapMail> getEmailsFromDb(Long userId, String accountEmail, String folderName, String query, int page, int size) {
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