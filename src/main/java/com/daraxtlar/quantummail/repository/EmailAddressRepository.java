package com.daraxtlar.quantummail.repository;

import com.daraxtlar.quantummail.entity.EmailAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository responsible for persistence operations related to
 * {@link EmailAddress} entities.
 *
 * <p>Provides methods for managing email account configurations
 * associated with application users.</p>
 */
@Repository
public interface EmailAddressRepository extends JpaRepository<EmailAddress, Long> {

    /**
     * Checks whether an email account already exists for a given user.
     *
     * @param emailAddress email account address
     * @param userId       user identifier
     * @return {@code true} if the account exists, otherwise {@code false}
     */
    boolean existsByEmailAddressAndUserId(String emailAddress, Long userId);

    /**
     * Retrieves all email accounts belonging to a user.
     *
     * @param userId user identifier
     * @return list of email accounts
     */
    List<EmailAddress> findByUserId(Long userId);

    /**
     * Finds a specific email account owned by a user.
     *
     * @param emailAddress email account address
     * @param userId       user identifier
     * @return matching email account if found
     */
    Optional<EmailAddress> findByEmailAddressAndUserId(String emailAddress, Long userId);
}
