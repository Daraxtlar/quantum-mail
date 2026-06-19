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

@Repository
public interface ImapMailRepository extends JpaRepository<ImapMail, Long> {

    Optional<ImapMail> findByAccountEmailAndFolderNameAndUid(String accountEmail, String folderName, String uid);

    Page<ImapMail> findByAccountEmailAndFolderNameOrderBySentDateDescIdDesc(
            String accountEmail, String folderName, Pageable pageable);

    boolean existsByAccountEmailAndFolderNameAndUid(String accountEmail, String folderName, String uid);

    Page<ImapMail> findByAccountEmailAndIsStarredTrueOrderBySentDateDescIdDesc(
            String accountEmail, Pageable pageable);

    long countByAccountEmailAndFolderName(String accountEmail, String folderName);

    @Query("SELECT m FROM ImapMail m WHERE m.accountEmail = :accountEmail AND m.folderName = :folderName " +
            "AND (LOWER(m.subject) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(m.sender) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(m.snippet) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY m.sentDate DESC, m.id DESC")
    Page<ImapMail> searchMails(@Param("accountEmail") String accountEmail,
                               @Param("folderName") String folderName,
                               @Param("query") String query,
                               Pageable pageable);

    Optional<ImapMail> findFirstByAccountEmailAndSenderAndSubjectAndSentDate(String accountEmail, String senderEmail, String subject, Date sentDate);

}
