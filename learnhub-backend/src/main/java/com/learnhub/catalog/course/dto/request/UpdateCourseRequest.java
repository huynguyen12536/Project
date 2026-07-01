package com.learnhub.catalog.course.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.UUID;

@Builder
public record UpdateCourseRequest(
    @Size(max = 255, message = "Tieu de khong qua 255 ky tu")
    String title,

    @Size(max = 500, message = "Phu de khong qua 500 ky tu")
    String subtitle,

    String description,
    String thumbnailUrl,
    String promoVideoUrl,
    UUID categoryId,
    UUID subcategoryId,
    UUID levelId,
    UUID languageId
) {}
