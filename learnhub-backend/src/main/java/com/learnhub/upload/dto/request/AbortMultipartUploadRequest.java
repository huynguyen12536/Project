package com.learnhub.upload.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AbortMultipartUploadRequest(
    @NotBlank(message = "uploadId la bat buoc")
    String uploadId,

    @NotBlank(message = "objectKey la bat buoc")
    String objectKey
) {}
