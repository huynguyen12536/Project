package com.learnhub.upload.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CompletedUploadPartRequest(
    @NotNull(message = "partNumber la bat buoc")
    @Positive(message = "partNumber phai lon hon 0")
    Integer partNumber,

    @NotBlank(message = "ETag la bat buoc")
    String eTag
) {}
