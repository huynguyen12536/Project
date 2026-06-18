package com.learnhub.auth.oauth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for HmacTokenHasher.
 * Verifies token hashing and verification logic.
 */
@ExtendWith(MockitoExtension.class)
class HmacTokenHasherTest {

    @Mock
    private GitHubOAuthProperties mockProperties;

    private HmacTokenHasher tokenHasher;
    private static final String TEST_SECRET = "test-secret-key-for-hmac-sha256";
    private static final String TEST_TOKEN = "test-oauth-token-value";

    @BeforeEach
    void setUp() {
        when(mockProperties.getTokenSecret()).thenReturn(TEST_SECRET);
        tokenHasher = new HmacTokenHasher(mockProperties);
    }

    @Test
    void testHash_GeneratesConsistentHash() {
        // Hash the same token twice
        String hash1 = tokenHasher.hash(TEST_TOKEN);
        String hash2 = tokenHasher.hash(TEST_TOKEN);

        // Both hashes should be identical (deterministic)
        assertEquals(hash1, hash2, "Same token should produce same hash");
    }

    @Test
    void testHash_DifferentTokensProduceDifferentHashes() {
        String hash1 = tokenHasher.hash("token1");
        String hash2 = tokenHasher.hash("token2");

        assertNotEquals(hash1, hash2, "Different tokens should produce different hashes");
    }

    @Test
    void testHash_OutputLength() {
        String hash = tokenHasher.hash(TEST_TOKEN);

        // SHA-256 produces 64-character hex string
        assertEquals(64, hash.length(), "SHA-256 hash should be 64 characters");
        assertEquals(64, tokenHasher.getHashLength(), "getHashLength should return 64");
    }

    @Test
    void testVerify_CorrectTokenMatches() {
        String hash = tokenHasher.hash(TEST_TOKEN);
        boolean result = tokenHasher.verify(TEST_TOKEN, hash);

        assertTrue(result, "Correct token should verify successfully");
    }

    @Test
    void testVerify_IncorrectTokenFails() {
        String hash = tokenHasher.hash(TEST_TOKEN);
        boolean result = tokenHasher.verify("wrong-token", hash);

        assertFalse(result, "Incorrect token should fail verification");
    }

    @Test
    void testVerify_EmptyTokenFails() {
        String hash = tokenHasher.hash(TEST_TOKEN);
        boolean result = tokenHasher.verify("", hash);

        assertFalse(result, "Empty token should fail verification");
    }

    @Test
    void testVerify_ConstantTimeComparison() {
        // Test timing-safe comparison by verifying both correct and incorrect tokens
        String hash = tokenHasher.hash(TEST_TOKEN);

        // Both should return without timing differences
        long start1 = System.nanoTime();
        tokenHasher.verify(TEST_TOKEN, hash);
        long duration1 = System.nanoTime() - start1;

        long start2 = System.nanoTime();
        tokenHasher.verify("wrong-token", hash);
        long duration2 = System.nanoTime() - start2;

        // Times should be roughly similar (constant-time comparison)
        // Note: This is not a perfect timing test, just a sanity check
        assertTrue(Math.abs(duration1 - duration2) < 1_000_000,
            "Constant-time comparison should not leak timing information");
    }

    @Test
    void testConstructor_ThrowsWhenSecretIsNull() {
        when(mockProperties.getTokenSecret()).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> new HmacTokenHasher(mockProperties),
            "Constructor should throw when secret is null");
    }

    @Test
    void testConstructor_ThrowsWhenSecretIsBlank() {
        when(mockProperties.getTokenSecret()).thenReturn("");

        assertThrows(IllegalStateException.class, () -> new HmacTokenHasher(mockProperties),
            "Constructor should throw when secret is blank");
    }
}
