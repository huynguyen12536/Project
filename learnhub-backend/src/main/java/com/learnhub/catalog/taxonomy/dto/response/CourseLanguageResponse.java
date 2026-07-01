package com.learnhub.catalog.taxonomy.dto.response;

import com.learnhub.catalog.taxonomy.model.CourseLanguage;
import lombok.Builder;

import java.util.UUID;

@Builder
public record CourseLanguageResponse(
    UUID id,
    String code,
    String label,
    Integer displayOrder,
    Boolean active
) {
    public static CourseLanguageResponse from(CourseLanguage language) {
        return CourseLanguageResponse.builder()
            .id(language.getId())
            .code(language.getCode())
            .label(language.getLabel())
            .displayOrder(language.getDisplayOrder())
            .active(language.getIsActive())
            .build();
    }
}
