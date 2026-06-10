package com.daraxtlar.quantummail.controller;

import com.daraxtlar.quantummail.entity.ImapMail;
import com.daraxtlar.quantummail.model.EmailMessage;
import com.daraxtlar.quantummail.service.JwtService;
import com.daraxtlar.quantummail.service.MailService;
import com.daraxtlar.quantummail.service.SendEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mails")
@CrossOrigin(origins = "http://localhost:5173")
public class MailController {
    private final SendEmailService sendEmailService;
    private final MailService mailService;

    @Autowired
    JwtService jwtService;

    public MailController(SendEmailService sendEmailService, MailService mailService) {
        this.sendEmailService = sendEmailService;
        this.mailService = mailService;
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendEmail(
            @RequestHeader("Authorization") String bearerToken,
            @RequestParam String senders,
            @RequestParam String[] recipients,
            @RequestParam String subject,
            @RequestParam String text,
            @RequestParam String method,
            @RequestParam(required = false) MultipartFile[] files,
            @RequestParam(required = false) String folderName,
            @RequestParam(required = false) Long parentMailId,
            @RequestParam(required = false) String actionType) {

        String token = bearerToken.substring(7);
        Long userId = jwtService.getUserIdFromToken(token);

        Boolean result = sendEmailService.sendEmail(userId ,senders, recipients, subject, text, method, files, folderName, parentMailId, actionType);

        if (result) {
            return ResponseEntity.ok(Map.of("message", "Email sent successfully"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to send email"));
        }
    }

    @GetMapping("/fetch")
    public ResponseEntity<?> fetchEmails(
            @RequestParam String accountEmail,
            @RequestParam(defaultValue = "INBOX") String folder,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String query) {

        Page<ImapMail> emailPage = mailService.getEmailsFromDb(accountEmail ,folder, query, page, size);

        return ResponseEntity.ok(Map.of(
                "emails", emailPage.getContent(),
                "totalCount", emailPage.getTotalElements(),
                "totalPages", emailPage.getTotalPages(),
                "currentPage", page,
                "folder", folder
                ));
    }

    @PostMapping("/sync")
    public ResponseEntity<?> syncEmails(
            @RequestParam String accountEmail,
            @RequestParam(defaultValue = "INBOX") String folder){
        mailService.syncFolderFromImap(accountEmail, folder);
        return ResponseEntity.ok(Map.of("message", "Folder synchronized successfully"));
    }

    @GetMapping("/{folder}/{uid}/attachments/{filename:.+}")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable String folder,
            @PathVariable long uid,
            @PathVariable String filename,
            @RequestParam(required = false, defaultValue = "false") boolean download) {
        byte[] data = mailService.downloadAttachment(folder, uid, filename);

        if (data == null) return ResponseEntity.noContent().build();

        String disposition = download ? "attachment" : "inline";

        MediaType mediaType = MediaTypeFactory.getMediaType(filename).orElse(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition +"; filename=\"" + filename + "\"")
                .body(data);
    }

    @GetMapping("/{accountEmail}/{folder}/{uid}")
    public ResponseEntity<EmailMessage> getEmailDetails(
            @PathVariable String accountEmail,
            @PathVariable String folder,
            @PathVariable long uid) {
        EmailMessage email = mailService.getEmailMessage(accountEmail ,folder, uid);

        if (email != null) {
            return ResponseEntity.ok(email);
        }else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> getSuggestions(
            @RequestHeader("Authorization") String bearerToken,
            @RequestParam String senderEmail) {
        String token = bearerToken.substring(7);
        Long userId = jwtService.getUserIdFromToken(token);

        return ResponseEntity.ok(mailService.getSuggestedRecipients(userId ,senderEmail));
    }

    @GetMapping("/suggestions/global")
    public ResponseEntity<List<String>> getGlobalSuggestions(@RequestHeader("Authorization") String bearerToken) {
        String token = bearerToken.substring(7);
        Long userId = jwtService.getUserIdFromToken(token);
        return ResponseEntity.ok(mailService.getGlobalSuggestedRecipients(userId));
    }

}
