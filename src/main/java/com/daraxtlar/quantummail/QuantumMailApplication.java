package com.daraxtlar.quantummail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the QuantumMail application.
 *
 * <p>Bootstraps the Spring Boot framework and initializes all
 * application components, configuration classes and services.</p>
 */
@SpringBootApplication
public class QuantumMailApplication {
    /**
     * Starts the QuantumMail application.
     *
     * @param args command-line arguments
     */
    static void main(String[] args) {
        SpringApplication.run(QuantumMailApplication.class, args);
    }
}