package com.learnhub.upload.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MultipartUploadSessionResponse(
    UUID sessionId,
    String uploadId,
    String objectKey,
    String bucket,
    String assetType,
    String fileName,
    String contentType,
    Long size,
    Long chunkSizeBytes,
    Integer maxConcurrency,
    Integer maxRetries,
    LocalDateTime expiresAt,
    List<UploadedPartResponse> uploadedParts
) {}
