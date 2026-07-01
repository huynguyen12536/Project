package com.learnhub.catalog.taxonomy.dto.response;

import com.learnhub.catalog.taxonomy.model.CourseTag;
import lombok.Builder;

import java.util.UUID;

@Builder
public record CourseTagResponse(
    UUID id,
    String name,
    String slug,
    Integer displayOrder,
    Boolean active
) {
    public static CourseTagResponse from(CourseTag tag) {
        return CourseTagResponse.builder()
            .id(tag.getId())
            .name(tag.getName())
            .slug(tag.getSlug())
            .displayOrder(tag.getDisplayOrder())
            .active(tag.getIsActive())
            .build();
    }
}
