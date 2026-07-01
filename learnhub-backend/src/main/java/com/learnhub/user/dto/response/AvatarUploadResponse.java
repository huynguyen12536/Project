package com.learnhub.user.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for avatar upload operation (POST /api/v1/users/{userId}/avatar).
 * Provides confirmation and new avatar URL to client.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvatarUploadResponse {

    private String message;

    private String avatarUrl;

    private Long fileSize;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant uploadedAt;
}
