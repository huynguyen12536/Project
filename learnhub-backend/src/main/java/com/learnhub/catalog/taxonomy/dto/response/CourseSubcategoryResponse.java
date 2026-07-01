package com.learnhub.catalog.taxonomy.dto.response;

import com.learnhub.catalog.taxonomy.model.CourseSubcategory;
import lombok.Builder;

import java.util.UUID;

@Builder
public record CourseSubcategoryResponse(
    UUID id,
    UUID categoryId,
    String categoryName,
    String name,
    String slug,
    String description,
    Integer displayOrder,
    Boolean active
) {
    public static CourseSubcategoryResponse from(CourseSubcategory subcategory) {
        return CourseSubcategoryResponse.builder()
            .id(subcategory.getId())
            .categoryId(subcategory.getCategory().getId())
            .categoryName(subcategory.getCategory().getName())
            .name(subcategory.getName())
            .slug(subcategory.getSlug())
            .description(subcategory.getDescription())
            .displayOrder(subcategory.getDisplayOrder())
            .active(subcategory.getIsActive())
            .build();
    }
}
