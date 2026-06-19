package com.daraxtlar.quantummail.repository;

import com.daraxtlar.quantummail.entity.EmailAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailAddressRepository extends JpaRepository<EmailAddress, Long> {
    boolean existsByEmailAddressAndUserId(String emailAddress, Long userId);

    List<EmailAddress> findByUserId(Long userId);

    Optional<EmailAddress> findByEmailAddressAndUserId(String emailAddress, Long userId);

}
