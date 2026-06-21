package com.daraxtlar.quantummail.repository;


import com.daraxtlar.quantummail.entity.ImapMail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

/**
 * Repository responsible for persistence operations related to
 * {@link ImapMail} entities.
 *
 * <p>Provides methods for storing, searching and retrieving
 * synchronized IMAP email messages.</p>
 */
@Repository
public interface ImapMailRepository extends JpaRepository<ImapMail, Long> {

    /**
     * Finds a message using its mailbox identifiers.
     *
     * @param accountEmail email account address
     * @param folderName   mailbox folder name
     * @param uid          message UID
     * @return matching email message if found
     */
    Optional<ImapMail> findByAccountEmailAndFolderNameAndUid(String accountEmail, String folderName, String uid);

    /**
     * Retrieves messages from a folder ordered by sending date.
     *
     * @param accountEmail email account address
     * @param folderName   mailbox folder name
     * @param pageable     pagination information
     * @return paginated collection of messages
     */
    Page<ImapMail> findByAccountEmailAndFolderNameOrderBySentDateDescIdDesc(
            String accountEmail, String folderName, Pageable pageable);

    /**
     * Retrieves starred messages for a given account.
     *
     * @param accountEmail email account address
     * @param pageable     pagination information
     * @return paginated collection of starred messages
     */
    Page<ImapMail> findByAccountEmailAndIsStarredTrueOrderBySentDateDescIdDesc(
            String accountEmail, Pageable pageable);

    /**
     * Counts messages stored within a specific mailbox folder.
     *
     * @param accountEmail email account address
     * @param folderName   mailbox folder name
     * @return number of stored messages
     */
    long countByAccountEmailAndFolderName(String accountEmail, String folderName);

    /**
     * Searches messages using subject, sender and snippet content.
     *
     * @param accountEmail email account address
     * @param folderName   mailbox folder name
     * @param query        search phrase
     * @param pageable     pagination information
     * @return paginated collection of matching messages
     */
    @Query("SELECT m FROM ImapMail m WHERE m.accountEmail = :accountEmail AND m.folderName = :folderName " +
            "AND (LOWER(m.subject) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(m.sender) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(m.snippet) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY m.sentDate DESC, m.id DESC")
    Page<ImapMail> searchMails(@Param("accountEmail") String accountEmail,
                               @Param("folderName") String folderName,
                               @Param("query") String query,
                               Pageable pageable);

    /**
     * Finds a message using its basic metadata.
     *
     * @param accountEmail email account address
     * @param senderEmail  sender email address
     * @param subject      message subject
     * @param sentDate     message sending date
     * @return matching email message if found
     */
    Optional<ImapMail> findFirstByAccountEmailAndSenderAndSubjectAndSentDate(String accountEmail, String senderEmail, String subject, Date sentDate);
}
