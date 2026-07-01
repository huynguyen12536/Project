package com.learnhub.catalog.course.dto.response;

import com.learnhub.catalog.course.model.CourseLecture;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record CourseLectureResponse(
    UUID id,
    UUID sectionId,
    UUID courseId,
    String title,
    CourseLecture.LectureType type,
    String content,
    String videoUrl,
    Integer durationSeconds,
    Integer displayOrder,
    Boolean isFreePreview,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static CourseLectureResponse from(CourseLecture lecture) {
        return CourseLectureResponse.builder()
            .id(lecture.getId())
            .sectionId(lecture.getSection().getId())
            .courseId(lecture.getCourse().getId())
            .title(lecture.getTitle())
            .type(lecture.getType())
            .content(lecture.getContent())
            .videoUrl(lecture.getVideoUrl())
            .durationSeconds(lecture.getDurationSeconds())
            .displayOrder(lecture.getDisplayOrder())
            .isFreePreview(lecture.getIsFreePreview())
            .createdAt(lecture.getCreatedAt())
            .updatedAt(lecture.getUpdatedAt())
            .build();
    }
}
