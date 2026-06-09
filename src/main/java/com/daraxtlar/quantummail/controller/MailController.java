package com.daraxtlar.quantummail.controller;

import com.daraxtlar.quantummail.model.EmailMessage;
import com.daraxtlar.quantummail.service.MailService;
import com.daraxtlar.quantummail.service.SendEmailService;
import jakarta.persistence.Id;
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

    public MailController(SendEmailService sendEmailService, MailService mailService) {
        this.sendEmailService = sendEmailService;
        this.mailService = mailService;
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendEmail(
            @RequestParam String senders,
            @RequestParam String[] recipients,
            @RequestParam String subject,
            @RequestParam String text,
            @RequestParam String method,
            @RequestParam(required = false) MultipartFile[] files,
            @RequestParam(required = false) String folderName,
            @RequestParam(required = false) Long parentMailId,
            @RequestParam(required = false) String actionType) {
        Boolean result = sendEmailService.sendEmail(senders, recipients, subject, text, method, files, folderName, parentMailId, actionType);

        if (result) {
            return ResponseEntity.ok(Map.of("message", "Email sent successfully"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to send email"));
        }
    }

    @GetMapping("/fetch")
    public ResponseEntity<?> fetchEmails(
            @RequestParam(defaultValue = "INBOX") String folder,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size){
        List<EmailMessage> emails = mailService.fetchEmails(folder, page, size);
        int totalCount = mailService.getFolderMessageCount(folder);
        int totalPages = (int) Math.ceil((double) totalCount / (double) size);

        return ResponseEntity.ok(Map.of(
                "emails", emails,
                "totalCount", totalCount,
                "totalPages", totalPages,
                "currentPage", page,
                "folder", folder
                ));
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

    @GetMapping("/{folder}/{uid}")
    public ResponseEntity<EmailMessage> getEmailDetails(
            @PathVariable String folder,
            @PathVariable long uid) {
        EmailMessage email = mailService.getEmailMessage(folder, uid);

        if (email != null) {
            return ResponseEntity.ok(email);
        }else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> getSuggestions(@RequestParam String senderEmail) {
        return ResponseEntity.ok(mailService.getSuggestedRecipients(senderEmail));
    }

    @GetMapping("/suggestions/global")
    public ResponseEntity<List<String>> getGlobalSuggestions() {
        return ResponseEntity.ok(mailService.getGlobalSuggestedRecipients());
    }

}
