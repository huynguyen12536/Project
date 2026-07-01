package com.learnhub.upload.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record CompleteMultipartUploadResponse(
    UUID assetId,
    UUID sessionId,
    String uploadId,
    String objectKey,
    String bucket,
    String publicUrl,
    String contentType,
    Long size,
    Integer durationSeconds,
    LocalDateTime completedAt
) {}
