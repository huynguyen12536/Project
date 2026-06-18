package com.learnhub.auth.oauth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TokenEncryptor (AES-256-GCM).
 * Verifies encryption and decryption of refresh tokens.
 */
class TokenEncryptorTest {

    private TokenEncryptor tokenEncryptor;

    @BeforeEach
    void setUp() {
        // Create encryptor with auto-generated key for testing
        tokenEncryptor = new TokenEncryptor("");
    }

    @Test
    void testEncrypt_ProducesEncryptedString() {
        String plaintext = "gho_16C7e42F292c6912E7710c838347Ae178B4a";

        String encrypted = tokenEncryptor.encrypt(plaintext);

        assertNotNull(encrypted, "Encrypted token should not be null");
        assertNotEquals(plaintext, encrypted, "Encrypted should differ from plaintext");
        assertTrue(encrypted.length() > 0, "Encrypted token should not be empty");
    }

    @Test
    void testDecrypt_ReturnsOriginalText() {
        String original = "gho_16C7e42F292c6912E7710c838347Ae178B4a";

        String encrypted = tokenEncryptor.encrypt(original);
        String decrypted = tokenEncryptor.decrypt(encrypted);

        assertEquals(original, decrypted, "Decrypted should match original");
    }

    @Test
    void testEncryptDecryptCycle_WithDifferentTokens() {
        String[] tokens = {
            "gho_16C7e42F292c6912E7710c838347Ae178B4a",
            "gho_another_refresh_token_abc123xyz789",
            "test_short",
            "test_with_special_chars_!@#$%^&*()"
        };

        for (String token : tokens) {
            String encrypted = tokenEncryptor.encrypt(token);
            String decrypted = tokenEncryptor.decrypt(encrypted);
            assertEquals(token, decrypted, "Should decrypt correctly: " + token);
        }
    }

    @Test
    void testEncrypt_GeneratesDifferentOutputEachTime() {
        String plaintext = "gho_16C7e42F292c6912E7710c838347Ae178B4a";

        String encrypted1 = tokenEncryptor.encrypt(plaintext);
        String encrypted2 = tokenEncryptor.encrypt(plaintext);

        assertNotEquals(encrypted1, encrypted2, "Each encryption should produce different output (different IV)");

        // But both should decrypt to same plaintext
        assertEquals(plaintext, tokenEncryptor.decrypt(encrypted1));
        assertEquals(plaintext, tokenEncryptor.decrypt(encrypted2));
    }

    @Test
    void testDecrypt_WithInvalidBase64_ThrowsException() {
        assertThrows(IllegalStateException.class, () -> {
            tokenEncryptor.decrypt("not-valid-base64-!");
        }, "Invalid base64 should throw exception");
    }

    @Test
    void testDecrypt_WithTamperedData_ThrowsException() {
        String original = "gho_16C7e42F292c6912E7710c838347Ae178B4a";
        String encrypted = tokenEncryptor.encrypt(original);

        // Tamper with encrypted data (change one character)
        String tampered = encrypted.substring(0, encrypted.length() - 1) + "X";

        assertThrows(IllegalStateException.class, () -> {
            tokenEncryptor.decrypt(tampered);
        }, "Tampered data should fail GCM authentication");
    }

    @Test
    void testEncrypt_EmptyString() {
        String encrypted = tokenEncryptor.encrypt("");
        String decrypted = tokenEncryptor.decrypt(encrypted);
        assertEquals("", decrypted, "Should handle empty strings");
    }

    @Test
    void testEncrypt_LongToken() {
        String longToken = "gho_" + "x".repeat(1000);
        String encrypted = tokenEncryptor.encrypt(longToken);
        String decrypted = tokenEncryptor.decrypt(encrypted);
        assertEquals(longToken, decrypted, "Should handle long tokens");
    }

    @Test
    void testEncrypt_UnicodeCharacters() {
        String tokenWithUnicode = "token_with_emoji_🔐_and_special_chars_你好";
        String encrypted = tokenEncryptor.encrypt(tokenWithUnicode);
        String decrypted = tokenEncryptor.decrypt(encrypted);
        assertEquals(tokenWithUnicode, decrypted, "Should handle unicode characters");
    }
}
