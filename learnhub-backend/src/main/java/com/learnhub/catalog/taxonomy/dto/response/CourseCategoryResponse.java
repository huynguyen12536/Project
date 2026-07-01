package com.learnhub.catalog.taxonomy.dto.response;

import com.learnhub.catalog.taxonomy.model.CourseCategory;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record CourseCategoryResponse(
    UUID id,
    String name,
    String slug,
    String description,
    Integer displayOrder,
    Boolean active,
    LocalDateTime createdAt
) {
    public static CourseCategoryResponse from(CourseCategory category) {
        return CourseCategoryResponse.builder()
            .id(category.getId())
            .name(category.getName())
            .slug(category.getSlug())
            .description(category.getDescription())
            .displayOrder(category.getDisplayOrder())
            .active(category.getIsActive())
            .createdAt(category.getCreatedAt())
            .build();
    }
}
