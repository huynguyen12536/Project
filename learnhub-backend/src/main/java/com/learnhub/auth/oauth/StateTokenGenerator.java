package com.learnhub.auth.oauth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * Generates and validates OAuth state tokens for CSRF protection.
 *
 * OAuth state token flow:
 * 1. Generate random state token (32 bytes)
 * 2. Store in Redis with 10-minute TTL
 * 3. Include in redirect URL to GitHub
 * 4. GitHub redirects back with same state token
 * 5. Validate state token matches stored value
 *
 * Security: State tokens must be cryptographically random and short-lived.
 */
@Component
@Slf4j
public class StateTokenGenerator {

    private static final int TOKEN_LENGTH = 32; // 32 bytes = 256 bits
    private static final String REDIS_PREFIX = "oauth:state:";
    private static final String USER_ID_PREFIX = "oauth:state:userid:";
    private static final Duration TTL = Duration.ofMinutes(15);

    private final RedisTemplate<String, String> redisTemplate;
    private final Random secureRandom = new SecureRandom();

    public StateTokenGenerator(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Generate a new cryptographically secure state token.
     * Stores it in Redis for validation during callback.
     *
     * @return The state token (32-byte hex string)
     */
    public String generate() {
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        String token = bytesToHex(tokenBytes);

        // Store in Redis with TTL
        String redisKey = REDIS_PREFIX + token;
        redisTemplate.opsForValue().set(redisKey, "valid", TTL);

        log.debug("Generated OAuth state token: {} (TTL: {})", token, TTL);
        return token;
    }

    /**
     * Validate that a state token is valid and hasn't expired.
     * Removes the token from Redis (one-time use).
     *
     * @param token The state token to validate
     * @return true if the token is valid, false if missing, expired, or invalid
     */
    public boolean validate(String token) {
        if (token == null || token.isBlank()) {
            log.warn("State token validation failed: token is empty");
            return false;
        }

        String redisKey = REDIS_PREFIX + token;

        // Attempt to retrieve and delete (one-time use)
        String value = redisTemplate.opsForValue().getAndDelete(redisKey);

        if (value != null) {
            log.debug("State token validation successful: {}", token);
            return true;
        } else {
            log.warn("State token validation failed: token not found or expired ({})", token);
            return false;
        }
    }

    /**
     * Generate a state token and bind it to a user ID.
     * Allows extraction of userId from state token during unauthenticated callback.
     *
     * @param userId The user ID to bind to this state token
     * @return The state token (32-byte hex string)
     */
    public String generateWithUserId(UUID userId) {
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        String token = bytesToHex(tokenBytes);

        String redisKey = REDIS_PREFIX + token;
        String userIdKey = USER_ID_PREFIX + token;

        // Store both token validity and userId mapping
        redisTemplate.opsForValue().set(redisKey, "valid", TTL);
        redisTemplate.opsForValue().set(userIdKey, userId.toString(), TTL);

        log.debug("Generated OAuth state token bound to user: {} (TTL: {})", userId, TTL);
        return token;
    }

    /**
     * Validate state token and retrieve associated user ID.
     * Removes both token and userId mapping from Redis (one-time use).
     *
     * @param token The state token to validate
     * @return Optional containing the user ID if valid, empty if missing/expired
     */
    public Optional<UUID> validateAndGetUserId(String token) {
        if (token == null || token.isBlank()) {
            log.warn("State token validation failed: token is empty");
            return Optional.empty();
        }

        String redisKey = REDIS_PREFIX + token;
        String userIdKey = USER_ID_PREFIX + token;

        try {
            // Retrieve and delete both token and userId (one-time use)
            String userIdStr = redisTemplate.opsForValue().getAndDelete(userIdKey);
            redisTemplate.opsForValue().getAndDelete(redisKey);

            if (userIdStr != null) {
                UUID userId = UUID.fromString(userIdStr);
                log.debug("State token validation successful, user ID extracted: {}", userId);
                return Optional.of(userId);
            } else {
                log.warn("State token validation failed: userId not found or expired ({})", token);
                return Optional.empty();
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format in state token mapping: {}", userIdStr, e);
            return Optional.empty();
        }
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
}
