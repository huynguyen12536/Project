package com.learnhub.catalog.course.dto.response;

import com.learnhub.catalog.course.model.Course;
import com.learnhub.catalog.course.model.Course.CourseStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record CourseResponse(
    UUID id,
    UUID instructorId,
    String title,
    String subtitle,
    String description,
    String thumbnailUrl,
    String promoVideoUrl,
    UUID categoryId,
    UUID subcategoryId,
    UUID levelId,
    UUID languageId,
    CourseStatus status,
    Integer totalVideoDurationSeconds,
    Integer lectureCount,
    Integer studentCount,
    BigDecimal averageRating,
    String rejectionReason,
    LocalDateTime publishedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static CourseResponse from(Course course) {
        return CourseResponse.builder()
            .id(course.getId())
            .instructorId(course.getInstructor().getId())
            .title(course.getTitle())
            .subtitle(course.getSubtitle())
            .description(course.getDescription())
            .thumbnailUrl(course.getThumbnailUrl())
            .promoVideoUrl(course.getPromoVideoUrl())
            .categoryId(course.getCategory() != null ? course.getCategory().getId() : null)
            .subcategoryId(course.getSubcategory() != null ? course.getSubcategory().getId() : null)
            .levelId(course.getLevel() != null ? course.getLevel().getId() : null)
            .languageId(course.getLanguage() != null ? course.getLanguage().getId() : null)
            .status(course.getStatus())
            .totalVideoDurationSeconds(course.getTotalVideoDurationSeconds())
            .lectureCount(course.getLectureCount())
            .studentCount(course.getStudentCount())
            .averageRating(course.getAverageRating())
            .rejectionReason(course.getRejectionReason())
            .publishedAt(course.getPublishedAt())
            .createdAt(course.getCreatedAt())
            .updatedAt(course.getUpdatedAt())
            .build();
    }
}
