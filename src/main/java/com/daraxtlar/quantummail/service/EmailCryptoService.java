package com.daraxtlar.quantummail.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Service responsible for encrypting and decrypting email account passwords.
 *
 * <p>Uses AES symmetric encryption and a secret key provided through
 * application configuration.</p>
 */
@Service
public class EmailCryptoService {
    private static final String ALGORITHM = "AES";

    @Value("${app.crypto.secret-key}")
    private String secretKey;

    /**
     * Encrypts a plain-text email password.
     *
     * @param rawPassword password to encrypt
     * @return encrypted password encoded as a Base64 string
     * @throws RuntimeException if encryption fails
     */
    public String encrypt(String rawPassword) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);

            byte[] encryptedBytes = cipher.doFinal(rawPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        }catch (Exception e) {
            throw new RuntimeException("Błąd podczas szyfrowania hasła email: ", e);
        }
    }

    /**
     * Decrypts a previously encrypted email password.
     *
     * @param encryptedPassword encrypted password encoded as Base64
     * @return decrypted plain-text password
     * @throws RuntimeException if decryption fails
     */
    public String decrypt(String encryptedPassword) {
        try{
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);

            byte[] decodedBytes = Base64.getDecoder().decode(encryptedPassword);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        }catch (Exception e) {
            throw new RuntimeException("Błąd podczas deszyfrowania hasła email: ", e);
        }
    }
}
