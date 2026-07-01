package com.learnhub.upload.dto.response;

import java.util.List;

public record MultipartUploadPresignResponse(
    String uploadId,
    String objectKey,
    List<PresignedPartResponse> parts
) {}
