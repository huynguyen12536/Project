package com.learnhub.upload.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CompleteMultipartUploadRequest(
    @NotBlank(message = "uploadId la bat buoc")
    String uploadId,

    @NotBlank(message = "objectKey la bat buoc")
    String objectKey,

    @Valid
    @NotEmpty(message = "Danh sach part da upload khong duoc rong")
    List<CompletedUploadPartRequest> parts,

    Integer durationSeconds
) {}
