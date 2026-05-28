package com.daraxtlar.quantummail.controller;

import com.daraxtlar.quantummail.model.EmailMessage;
import com.daraxtlar.quantummail.service.MailService;
import com.daraxtlar.quantummail.service.SendEmailService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
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
            @RequestParam(required = false) MultipartFile[] files) {
        Boolean result = sendEmailService.sendEmail(senders, recipients, subject, text, method, files);

        if (result) {
            return ResponseEntity.ok(Map.of("message", "Email sent successfully"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to send email"));
        }
    }

    @GetMapping("/fetch")
    public ResponseEntity<Map<String, Object>> fetchEmails() {
        List<EmailMessage> emails = mailService.fetchEmails();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("count", emails.size());
        response.put("emails", emails);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{uid}/attachments/{filename}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable long uid, @PathVariable String filename, @RequestParam(required = false, defaultValue = "false") boolean download) {
        byte[] data = mailService.downloadAttachment(uid, filename);

        if (data == null) return ResponseEntity.notFound().build();

        String disposition = download ? "attachment" : "inline";

        MediaType mediaType = MediaTypeFactory.getMediaType(filename).orElse(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition +"; filename=\"" + filename + "\"")
                .body(data);
    }

    @GetMapping("/{uid}")
    public ResponseEntity<EmailMessage> getEmailDetails(@PathVariable long uid) {
        EmailMessage email = mailService.getEmailMessage(uid);

        if (email != null) {
            return ResponseEntity.ok(email);
        }else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> refresh() {
        mailService.clearCache();
        return ResponseEntity.ok(Map.of("status", "Cache cleared"));
    }

}
