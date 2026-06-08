package com.daraxtlar.quantummail.repository;

import com.daraxtlar.quantummail.entity.Mail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MailRepository extends JpaRepository<Mail, Long> {

    Optional<Mail> findBySenderEmailAndRecipientEmail(String senderEmail, String recipientEmail);

    @Query("SELECT m.recipientEmail FROM Mail m WHERE m.senderEmail = :senderEmail ORDER BY m.sentDate DESC")
    List<String> findRecentRecipients(@Param("senderEmail") String senderEmail);

    @Modifying
    @Query("DELETE FROM Mail m WHERE m.sentDate < :thresholdDate")
    void deleteOlderThan(@Param("thresholdDate") LocalDateTime thresholdDate);
}
