package com.learnhub.auth.oauth.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for OAuth connection status.
 */
public record UserOAuthResponse(
    UUID userId,
    String githubUsername,
    String avatarUrl,
    LocalDateTime connectedAt
) {
    /**
     * Backward-compatible factory for existing callers that don't have userId.
     */
    public static UserOAuthResponse withoutUserId(String githubUsername, String avatarUrl, LocalDateTime connectedAt) {
        return new UserOAuthResponse(null, githubUsername, avatarUrl, connectedAt);
    }
}
