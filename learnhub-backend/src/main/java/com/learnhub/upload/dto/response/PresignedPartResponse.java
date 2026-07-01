package com.learnhub.upload.dto.response;

public record PresignedPartResponse(
    Integer partNumber,
    String url
) {}
