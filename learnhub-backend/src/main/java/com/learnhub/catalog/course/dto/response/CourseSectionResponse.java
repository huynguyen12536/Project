package com.learnhub.catalog.course.dto.response;

import com.learnhub.catalog.course.model.CourseSection;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Builder
public record CourseSectionResponse(
    UUID id,
    UUID courseId,
    String title,
    Integer displayOrder,
    List<CourseLectureResponse> lectures,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static CourseSectionResponse from(CourseSection section) {
        return CourseSectionResponse.builder()
            .id(section.getId())
            .courseId(section.getCourse().getId())
            .title(section.getTitle())
            .displayOrder(section.getDisplayOrder())
            .lectures(section.getLectures() != null ? section.getLectures().stream()
                .map(CourseLectureResponse::from)
                .collect(Collectors.toList()) : List.of())
            .createdAt(section.getCreatedAt())
            .updatedAt(section.getUpdatedAt())
            .build();
    }
}
