package com.learnhub.upload.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PresignMultipartUploadRequest(
    @NotBlank(message = "uploadId la bat buoc")
    String uploadId,

    @NotBlank(message = "objectKey la bat buoc")
    String objectKey,

    @NotEmpty(message = "Can it nhat 1 partNumber")
    List<Integer> partNumbers
) {}
