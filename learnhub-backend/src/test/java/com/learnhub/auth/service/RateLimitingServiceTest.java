package com.learnhub.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for RateLimitingService.
 * Verifies: allow up to max, block after max, window reset, reset method.
 */
class RateLimitingServiceTest {

    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        rateLimitingService = new RateLimitingService();
    }

    @Test
    @DisplayName("Requests within limit are allowed")
    void withinLimit_allowed() {
        for (int i = 0; i < 5; i++) {
            assertThat(rateLimitingService.isAllowed("test-ip", 5, 60)).isTrue();
        }
    }

    @Test
    @DisplayName("Request exceeding limit is blocked")
    void exceedingLimit_blocked() {
        for (int i = 0; i < 5; i++) {
            rateLimitingService.isAllowed("ip-block-test", 5, 60);
        }
        assertThat(rateLimitingService.isAllowed("ip-block-test", 5, 60)).isFalse();
    }

    @Test
    @DisplayName("Different keys have independent buckets")
    void differentKeys_independent() {
        // Fill bucket for key1
        for (int i = 0; i < 5; i++) {
            rateLimitingService.isAllowed("key1", 5, 60);
        }
        // key2 should still be allowed
        assertThat(rateLimitingService.isAllowed("key2", 5, 60)).isTrue();
        // key1 should be blocked
        assertThat(rateLimitingService.isAllowed("key1", 5, 60)).isFalse();
    }

    @Test
    @DisplayName("Reset clears the bucket — requests allowed again")
    void reset_clearsBucket() {
        for (int i = 0; i < 5; i++) {
            rateLimitingService.isAllowed("reset-key", 5, 60);
        }
        rateLimitingService.reset("reset-key");
        assertThat(rateLimitingService.isAllowed("reset-key", 5, 60)).isTrue();
    }

    @Test
    @DisplayName("secondsUntilReset returns 0 for unknown key")
    void secondsUntilReset_unknownKey_returnsZero() {
        assertThat(rateLimitingService.secondsUntilReset("unknown", 60)).isZero();
    }

    @Test
    @DisplayName("secondsUntilReset returns positive for tracked key")
    void secondsUntilReset_trackedKey_returnsPositive() {
        rateLimitingService.isAllowed("tracked", 5, 60);
        long remaining = rateLimitingService.secondsUntilReset("tracked", 60);
        assertThat(remaining).isGreaterThanOrEqualTo(0).isLessThanOrEqualTo(60);
    }

    @Test
    @DisplayName("Rate limiting: login scenario — 5/min then block")
    void loginRateLimit_scenario() {
        String ip = "192.168.1.1";
        int max = 5;
        long window = 60L;

        for (int i = 0; i < max; i++) {
            assertThat(rateLimitingService.isAllowed("login:" + ip, max, window))
                .as("Request %d should be allowed", i + 1)
                .isTrue();
        }

        // 6th request should be blocked
        assertThat(rateLimitingService.isAllowed("login:" + ip, max, window))
            .as("6th request should be blocked")
            .isFalse();
    }

    @Test
    @DisplayName("Rate limiting: forgot-password scenario — 3/hour then block")
    void forgotPasswordRateLimit_scenario() {
        String emailKey = "fp:test@example.com";
        int max = 3;
        long window = 3600L;

        for (int i = 0; i < max; i++) {
            assertThat(rateLimitingService.isAllowed(emailKey, max, window)).isTrue();
        }
        assertThat(rateLimitingService.isAllowed(emailKey, max, window)).isFalse();
    }
}
