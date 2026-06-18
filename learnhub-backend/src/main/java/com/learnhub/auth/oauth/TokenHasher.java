package com.learnhub.auth.oauth;

/**
 * Interface for secure token hashing and verification.
 * Tokens (access tokens, refresh tokens, state tokens) are never stored in plaintext.
 * They are hashed using HMAC-SHA256 with a secure secret.
 *
 * This abstraction allows future implementations (BCrypt, Argon2, etc.) without
 * changing the rest of the codebase.
 */
public interface TokenHasher {

    /**
     * Hash a raw token using HMAC-SHA256.
     *
     * @param rawToken The plaintext token to hash
     * @return The hashed token (hex-encoded)
     */
    String hash(String rawToken);

    /**
     * Verify that a raw token matches a previously hashed value.
     *
     * @param rawToken The plaintext token to verify
     * @param hash The stored hash to compare against
     * @return true if the token matches the hash, false otherwise
     */
    boolean verify(String rawToken, String hash);

    /**
     * Get the expected output length of hashes (for column sizing).
     *
     * @return The hash output length in characters
     */
    int getHashLength();
}
