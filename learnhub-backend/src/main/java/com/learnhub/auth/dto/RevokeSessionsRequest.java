package com.learnhub.auth.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RevokeSessionsRequest(
    @NotNull(message = "User ID is required")
    UUID userId
) {}
