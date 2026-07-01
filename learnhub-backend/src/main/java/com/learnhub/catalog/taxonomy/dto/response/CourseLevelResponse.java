package com.learnhub.catalog.taxonomy.dto.response;

import com.learnhub.catalog.taxonomy.model.CourseLevel;
import lombok.Builder;

import java.util.UUID;

@Builder
public record CourseLevelResponse(
    UUID id,
    String code,
    String label,
    String description,
    Integer displayOrder,
    Boolean active
) {
    public static CourseLevelResponse from(CourseLevel level) {
        return CourseLevelResponse.builder()
            .id(level.getId())
            .code(level.getCode())
            .label(level.getLabel())
            .description(level.getDescription())
            .displayOrder(level.getDisplayOrder())
            .active(level.getIsActive())
            .build();
    }
}
