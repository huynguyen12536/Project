package com.learnhub.upload.dto.response;

public record UploadedPartResponse(
    Integer partNumber,
    String eTag,
    Long size
) {}
