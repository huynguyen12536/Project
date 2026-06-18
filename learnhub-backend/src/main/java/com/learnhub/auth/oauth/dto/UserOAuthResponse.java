package com.learnhub.auth.oauth.dto;

import java.time.LocalDateTime;

/**
 * Response DTO for OAuth connection status.
 */
public record UserOAuthResponse(
    String githubUsername,
    String avatarUrl,
    LocalDateTime connectedAt
) {}
