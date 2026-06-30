package com.learnhub.auth.dto;

import java.util.UUID;

public record AuthResponse(
    UUID userId,
    String role,
    String email,
    String token,
    String refreshToken,
    long expiresIn
) {}
