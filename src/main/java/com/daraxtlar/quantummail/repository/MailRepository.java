package com.daraxtlar.quantummail.repository;

import com.daraxtlar.quantummail.entity.Mail;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MailRepository extends JpaRepository<Mail, Long> {

    Optional<Mail> findBySenderEmailAndRecipientEmailAndUserId(String senderEmail, String recipientEmail, Long userId);

    @Query("SELECT m.recipientEmail FROM Mail m " +
            "WHERE m.user.id = :userId AND m.senderEmail = :senderEmail " +
            "GROUP BY m.recipientEmail " +
            "ORDER BY MAX(m.sentDate) DESC")
    List<String> findRecentRecipientsByEmail(
            @Param("userId") Long userId,
            @Param("senderEmail") String senderEmail,
            Pageable pageable);

    @Modifying
    @Query("DELETE FROM Mail m WHERE m.sentDate < :thresholdDate")
    void deleteOlderThan(@Param("thresholdDate") LocalDateTime thresholdDate);

    @Query("SELECT m.recipientEmail FROM Mail m " +
            "WHERE m.user.id = :userId " +
            "GROUP BY m.recipientEmail " +
            "ORDER BY MAX(m.sentDate) DESC")
    List<String> findRecentRecipientsByAccount(
            @Param("userId") Long userId,
            Pageable pageable);
}
