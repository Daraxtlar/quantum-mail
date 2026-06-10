package com.daraxtlar.quantummail.repository;

import com.daraxtlar.quantummail.entity.EmailAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailAddressRepository extends JpaRepository<EmailAddress, Long> {
    boolean existsByEmailAddressAndUserId(String emailAddress, Long userId);
}
