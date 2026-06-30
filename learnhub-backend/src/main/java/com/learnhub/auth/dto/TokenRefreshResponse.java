package com.learnhub.auth.dto;

public record TokenRefreshResponse(
    String token,
    String refreshToken,
    long expiresIn
) {}
