package com.learnhub.auth.oauth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * HMAC-SHA256 implementation of TokenHasher.
 * Uses a secure secret (from environment) to hash tokens.
 *
 * Security Note:
 * - Tokens are hashed with HMAC-SHA256, not simple SHA-256
 * - The secret is loaded from GITHUB_TOKEN_SECRET environment variable
 * - Never hash tokens without a secret (which is what plain SHA-256 would do)
 */
@Component
@Slf4j
public class HmacTokenHasher implements TokenHasher {

    private static final String ALGORITHM = "HmacSHA256";
    private static final int HASH_LENGTH = 64; // SHA-256 hex output is 64 chars

    private final String secret;

    public HmacTokenHasher(GitHubOAuthProperties gitHubOAuthProperties) {
        this.secret = gitHubOAuthProperties.getTokenSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                "GITHUB_TOKEN_SECRET environment variable is required for token hashing. " +
                "Generate with: openssl rand -base64 32"
            );
        }
    }

    @Override
    public String hash(String rawToken) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                0,
                secret.getBytes(StandardCharsets.UTF_8).length,
                ALGORITHM
            );
            mac.init(keySpec);

            byte[] hash = mac.doFinal(rawToken.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to hash token", e);
            throw new RuntimeException("Token hashing failed", e);
        }
    }

    @Override
    public boolean verify(String rawToken, String hash) {
        try {
            String computedHash = hash(rawToken);
            // Constant-time comparison to prevent timing attacks
            return constantTimeEquals(computedHash, hash);
        } catch (Exception e) {
            log.warn("Token verification failed", e);
            return false;
        }
    }

    @Override
    public int getHashLength() {
        return HASH_LENGTH;
    }

    /**
     * Convert byte array to hex string.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     * Compares two strings character-by-character, always checking all characters
     * regardless of where the mismatch occurs.
     */
    private boolean constantTimeEquals(String a, String b) {
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);

        int result = 0;
        if (aBytes.length != bBytes.length) {
            return false;
        }

        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }

        return result == 0;
    }
}
