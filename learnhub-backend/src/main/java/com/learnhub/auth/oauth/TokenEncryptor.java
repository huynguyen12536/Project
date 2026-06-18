package com.learnhub.auth.oauth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encrypts and decrypts refresh tokens using AES-256-GCM.
 * AES-GCM provides authenticated encryption (both confidentiality and integrity).
 *
 * Key derivation from password:
 * - Uses environment variable GITHUB_TOKEN_ENCRYPTION_KEY (must be exactly 32 bytes for AES-256)
 * - Or generates a new key if not provided (development only)
 *
 * Encryption format:
 * - IV (12 bytes) + Ciphertext + Auth Tag (16 bytes) → Base64 encoded
 */
@Component
@Slf4j
public class TokenEncryptor {

    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256; // bits
    private static final int IV_SIZE = 12; // 96 bits for GCM
    private static final int AUTH_TAG_SIZE = 128; // 128 bits = 16 bytes
    private static final String ENCRYPTION_KEY_ENV = "GITHUB_TOKEN_ENCRYPTION_KEY";

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenEncryptor(@Value("${github.token.encryption-key:}") String encryptionKeyEnv) {
        this.secretKey = initializeKey(encryptionKeyEnv);
    }

    /**
     * Initialize encryption key from environment variable or generate a new one.
     *
     * @param keyString Base64-encoded or hex-encoded encryption key from environment
     * @return Secret key for AES encryption
     */
    private SecretKey initializeKey(String keyString) {
        if (keyString != null && !keyString.isBlank()) {
            try {
                byte[] decodedKey;
                // Try base64 first
                try {
                    decodedKey = Base64.getDecoder().decode(keyString);
                } catch (IllegalArgumentException e) {
                    // Try hex
                    decodedKey = hexStringToByteArray(keyString);
                }

                if (decodedKey.length != 32) {
                    throw new IllegalArgumentException(
                        "Encryption key must be exactly 32 bytes (256 bits), got " + decodedKey.length
                    );
                }

                log.info("Loaded encryption key from environment variable");
                return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
            } catch (Exception e) {
                log.error("Failed to parse encryption key from environment", e);
                throw new IllegalStateException("Invalid encryption key configuration", e);
            }
        } else {
            // Generate new key for development
            log.warn("No encryption key provided, generating development key (NOT FOR PRODUCTION)");
            try {
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(KEY_SIZE, secureRandom);
                return keyGen.generateKey();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to generate encryption key", e);
            }
        }
    }

    /**
     * Encrypt a refresh token using AES-256-GCM.
     *
     * @param plaintext The plain refresh token
     * @return Base64-encoded encrypted token (IV + Ciphertext + Auth Tag)
     */
    public String encrypt(String plaintext) {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);

            // Generate random IV
            byte[] iv = new byte[IV_SIZE];
            secureRandom.nextBytes(iv);

            // Initialize cipher with IV
            GCMParameterSpec gcmSpec = new GCMParameterSpec(AUTH_TAG_SIZE, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            // Encrypt the plaintext
            byte[] plainBytes = plaintext.getBytes(StandardCharsets.UTF_8);
            byte[] ciphertext = cipher.doFinal(plainBytes);

            // Combine IV + Ciphertext (already includes auth tag)
            ByteBuffer buffer = ByteBuffer.allocate(IV_SIZE + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);

            // Return as Base64
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new IllegalStateException("Failed to encrypt token", e);
        }
    }

    /**
     * Decrypt a refresh token using AES-256-GCM.
     *
     * @param encryptedBase64 Base64-encoded encrypted token
     * @return Plain refresh token
     */
    public String decrypt(String encryptedBase64) {
        try {
            // Decode from Base64
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedBase64);

            // Extract IV and ciphertext
            ByteBuffer buffer = ByteBuffer.wrap(encryptedBytes);
            byte[] iv = new byte[IV_SIZE];
            buffer.get(iv);

            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            // Initialize cipher with IV
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(AUTH_TAG_SIZE, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            // Decrypt
            byte[] plainBytes = cipher.doFinal(ciphertext);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new IllegalStateException("Failed to decrypt token", e);
        }
    }

    /**
     * Convert hex string to byte array.
     */
    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
