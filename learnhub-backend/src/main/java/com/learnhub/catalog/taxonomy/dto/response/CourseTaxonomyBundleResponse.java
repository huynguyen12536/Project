package com.learnhub.catalog.taxonomy.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record CourseTaxonomyBundleResponse(
    List<CourseCategoryResponse> categories,
    List<CourseSubcategoryResponse> subcategories,
    List<CourseLevelResponse> levels,
    List<CourseLanguageResponse> languages,
    List<CourseTagResponse> tags
) {}
