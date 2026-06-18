package com.learnhub.auth.oauth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for StateTokenGenerator.
 * Verifies CSRF state token generation and validation.
 */
@ExtendWith(MockitoExtension.class)
class StateTokenGeneratorTest {

    @Mock
    private RedisTemplate<String, String> mockRedisTemplate;

    @Mock
    private ValueOperations<String, String> mockValueOps;

    private StateTokenGenerator stateTokenGenerator;

    @BeforeEach
    void setUp() {
        when(mockRedisTemplate.opsForValue()).thenReturn(mockValueOps);
        stateTokenGenerator = new StateTokenGenerator(mockRedisTemplate);
    }

    @Test
    void testGenerate_CreatesValidToken() {
        // Act
        String token = stateTokenGenerator.generate();

        // Assert
        assertNotNull(token, "Generated token should not be null");
        assertFalse(token.isBlank(), "Generated token should not be blank");
        assertEquals(64, token.length(), "Token should be 64 characters (32 bytes hex)");
    }

    @Test
    void testGenerate_CreatesUniqueTokens() {
        // Act
        String token1 = stateTokenGenerator.generate();
        String token2 = stateTokenGenerator.generate();

        // Assert
        assertNotEquals(token1, token2, "Each generated token should be unique");
    }

    @Test
    void testGenerate_StoresTokenInRedis() {
        // Act
        stateTokenGenerator.generate();

        // Assert
        verify(mockValueOps).set(anyString(), eq("valid"), eq(Duration.ofMinutes(10)));
    }

    @Test
    void testValidate_AcceptsValidToken() {
        // Arrange
        String token = "validtoken123456789012345678901234567890";
        when(mockValueOps.getAndDelete("oauth:state:" + token)).thenReturn("valid");

        // Act
        boolean result = stateTokenGenerator.validate(token);

        // Assert
        assertTrue(result, "Valid token should pass validation");
    }

    @Test
    void testValidate_RejectsInvalidToken() {
        // Arrange
        String token = "invalidtoken";
        when(mockValueOps.getAndDelete(anyString())).thenReturn(null);

        // Act
        boolean result = stateTokenGenerator.validate(token);

        // Assert
        assertFalse(result, "Invalid token should fail validation");
    }

    @Test
    void testValidate_RejectsNullToken() {
        // Act
        boolean result = stateTokenGenerator.validate(null);

        // Assert
        assertFalse(result, "Null token should fail validation");
    }

    @Test
    void testValidate_RejectsBlankToken() {
        // Act
        boolean result = stateTokenGenerator.validate("");

        // Assert
        assertFalse(result, "Blank token should fail validation");
    }

    @Test
    void testValidate_IsOneTimeUse() {
        // Arrange
        String token = "validtoken123456789012345678901234567890";
        when(mockValueOps.getAndDelete("oauth:state:" + token)).thenReturn("valid");

        // Act - First validation succeeds
        boolean result1 = stateTokenGenerator.validate(token);
        assertTrue(result1, "First validation should succeed");

        // Act - Second validation with same token fails (one-time use)
        when(mockValueOps.getAndDelete("oauth:state:" + token)).thenReturn(null);
        boolean result2 = stateTokenGenerator.validate(token);
        assertFalse(result2, "Second validation of same token should fail (one-time use)");
    }

    @Test
    void testGenerateWithUserId_CreatesValidToken() {
        // Arrange
        UUID userId = UUID.randomUUID();

        // Act
        String token = stateTokenGenerator.generateWithUserId(userId);

        // Assert
        assertNotNull(token, "Generated token should not be null");
        assertFalse(token.isBlank(), "Generated token should not be blank");
        assertEquals(64, token.length(), "Token should be 64 characters");
    }

    @Test
    void testGenerateWithUserId_StoresUserIdMapping() {
        // Arrange
        UUID userId = UUID.randomUUID();

        // Act
        stateTokenGenerator.generateWithUserId(userId);

        // Assert
        verify(mockValueOps).set(anyString(), eq("valid"), eq(Duration.ofMinutes(15)));
        verify(mockValueOps).set(
            eq("oauth:state:userid:" + anyString()),
            eq(userId.toString()),
            eq(Duration.ofMinutes(15))
        );
    }

    @Test
    void testValidateAndGetUserId_ReturnsUserIdWhenValid() {
        // Arrange
        String token = "validtoken1234567890123456789012345678901";
        UUID expectedUserId = UUID.randomUUID();
        when(mockValueOps.getAndDelete("oauth:state:userid:" + token))
            .thenReturn(expectedUserId.toString());
        when(mockValueOps.getAndDelete("oauth:state:" + token))
            .thenReturn("valid");

        // Act
        Optional<UUID> result = stateTokenGenerator.validateAndGetUserId(token);

        // Assert
        assertTrue(result.isPresent(), "Should return userId when valid");
        assertEquals(expectedUserId, result.get(), "Should return correct userId");
    }

    @Test
    void testValidateAndGetUserId_ReturnsEmptyWhenInvalid() {
        // Arrange
        String token = "invalidtoken";
        when(mockValueOps.getAndDelete(anyString())).thenReturn(null);

        // Act
        Optional<UUID> result = stateTokenGenerator.validateAndGetUserId(token);

        // Assert
        assertTrue(result.isEmpty(), "Should return empty when token invalid");
    }

    @Test
    void testValidateAndGetUserId_ReturnsEmptyForNullToken() {
        // Act
        Optional<UUID> result = stateTokenGenerator.validateAndGetUserId(null);

        // Assert
        assertTrue(result.isEmpty(), "Should return empty for null token");
    }

    @Test
    void testValidateAndGetUserId_ReturnsEmptyForBlankToken() {
        // Act
        Optional<UUID> result = stateTokenGenerator.validateAndGetUserId("");

        // Assert
        assertTrue(result.isEmpty(), "Should return empty for blank token");
    }

    @Test
    void testValidateAndGetUserId_IsOneTimeUse() {
        // Arrange
        String token = "validtoken1234567890123456789012345678901";
        UUID expectedUserId = UUID.randomUUID();
        when(mockValueOps.getAndDelete("oauth:state:userid:" + token))
            .thenReturn(expectedUserId.toString());
        when(mockValueOps.getAndDelete("oauth:state:" + token))
            .thenReturn("valid");

        // Act - First validation succeeds
        Optional<UUID> result1 = stateTokenGenerator.validateAndGetUserId(token);
        assertTrue(result1.isPresent(), "First validation should succeed");

        // Act - Second validation fails (one-time use)
        when(mockValueOps.getAndDelete(anyString())).thenReturn(null);
        Optional<UUID> result2 = stateTokenGenerator.validateAndGetUserId(token);
        assertTrue(result2.isEmpty(), "Second validation should fail (one-time use)");
    }

    @Test
    void testValidateAndGetUserId_HandleInvalidUUID() {
        // Arrange
        String token = "validtoken1234567890123456789012345678901";
        when(mockValueOps.getAndDelete("oauth:state:userid:" + token))
            .thenReturn("not-a-valid-uuid");
        when(mockValueOps.getAndDelete("oauth:state:" + token))
            .thenReturn("valid");

        // Act
        Optional<UUID> result = stateTokenGenerator.validateAndGetUserId(token);

        // Assert
        assertTrue(result.isEmpty(), "Should return empty for invalid UUID format");
    }
}
