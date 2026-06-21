package com.daraxtlar.quantummail.repository;

import com.daraxtlar.quantummail.entity.Mail;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository responsible for persistence operations related to
 * {@link Mail} entities.
 *
 * <p>Used for managing recipient suggestions and communication history
 * associated with application users.</p>
 */
public interface MailRepository extends JpaRepository<Mail, Long> {

    /**
     * Finds a sender-recipient relationship for a specific user.
     *
     * @param senderEmail    sender email address
     * @param recipientEmail recipient email address
     * @param userId         user identifier
     * @return matching relationship if found
     */
    Optional<Mail> findBySenderEmailAndRecipientEmailAndUserId(String senderEmail, String recipientEmail, Long userId);

    /**
     * Retrieves the most recently used recipients for a given sender address.
     *
     * @param userId      user identifier
     * @param senderEmail sender email address
     * @param pageable    pagination information
     * @return list of recipient email addresses
     */
    @Query("SELECT m.recipientEmail FROM Mail m " +
            "WHERE m.user.id = :userId AND m.senderEmail = :senderEmail " +
            "GROUP BY m.recipientEmail " +
            "ORDER BY MAX(m.sentDate) DESC")
    List<String> findRecentRecipientsByEmail(
            @Param("userId") Long userId,
            @Param("senderEmail") String senderEmail,
            Pageable pageable);

    /**
     * Retrieves the most recently used recipients across all user accounts.
     *
     * @param userId   user identifier
     * @param pageable pagination information
     * @return list of recipient email addresses
     */
    @Query("SELECT m.recipientEmail FROM Mail m " +
            "WHERE m.user.id = :userId " +
            "GROUP BY m.recipientEmail " +
            "ORDER BY MAX(m.sentDate) DESC")
    List<String> findRecentRecipientsByAccount(
            @Param("userId") Long userId,
            Pageable pageable);
}
