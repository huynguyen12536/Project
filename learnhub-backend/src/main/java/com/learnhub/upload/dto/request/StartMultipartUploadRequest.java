package com.learnhub.upload.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record StartMultipartUploadRequest(
    @NotBlank(message = "Ten tep la bat buoc")
    String fileName,

    @NotBlank(message = "Content-Type la bat buoc")
    String contentType,

    @NotNull(message = "Kich thuoc tep la bat buoc")
    @Positive(message = "Kich thuoc tep phai lon hon 0")
    Long size,

    @NotBlank(message = "Loai asset la bat buoc")
    String assetType,

    @NotNull(message = "courseId la bat buoc")
    UUID courseId,

    UUID lectureId
) {}
