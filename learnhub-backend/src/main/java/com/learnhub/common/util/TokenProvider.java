package com.learnhub.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@Component
@Slf4j
public class TokenProvider {

    private static final int TOKEN_LENGTH = 32;
    private static final SecureRandom secureRandom = new SecureRandom();

    public String generateVerificationToken(UUID userId) {
        return generateSecureToken();
    }

    public String generateResetToken(UUID userId) {
        return generateSecureToken();
    }

    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Error hashing token", e);
            throw new RuntimeException("Error hashing token", e);
        }
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
