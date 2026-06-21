package com.daraxtlar.quantummail.repository;

import com.daraxtlar.quantummail.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository responsible for persistence operations related to
 * {@link User} entities.
 *
 * <p>Provides methods for retrieving and managing application users.</p>
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by username.
     *
     * @param username username used for authentication
     * @return matching user if found
     */
    Optional<User> findByUsername(String username);
}