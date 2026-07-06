package com.learnhub.catalog.course.dto.response;

import com.learnhub.catalog.course.model.Course;
import com.learnhub.catalog.course.model.Course.CourseStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Builder
public record CourseDetailResponse(
    UUID id,
    UUID instructorId,
    String instructorName,
    String title,
    String subtitle,
    String description,
    String thumbnailUrl,
    String promoVideoUrl,
    UUID categoryId,
    String categoryName,
    UUID subcategoryId,
    UUID levelId,
    UUID languageId,
    String languageName,
    CourseStatus status,
    Integer totalVideoDurationSeconds,
    Integer lectureCount,
    Integer studentCount,
    BigDecimal averageRating,
    String rejectionReason,
    LocalDateTime publishedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<CourseSectionResponse> sections
) {
    public static CourseDetailResponse from(Course course) {
        return CourseDetailResponse.builder()
            .id(course.getId())
            .instructorId(course.getInstructor().getId())
            .instructorName(course.getInstructor().getUsername())
            .title(course.getTitle())
            .subtitle(course.getSubtitle())
            .description(course.getDescription())
            .thumbnailUrl(course.getThumbnailUrl())
            .promoVideoUrl(course.getPromoVideoUrl())
            .categoryId(course.getCategory() != null ? course.getCategory().getId() : null)
            .categoryName(course.getCategory() != null ? course.getCategory().getName() : null)
            .subcategoryId(course.getSubcategory() != null ? course.getSubcategory().getId() : null)
            .levelId(course.getLevel() != null ? course.getLevel().getId() : null)
            .languageId(course.getLanguage() != null ? course.getLanguage().getId() : null)
            .languageName(course.getLanguage() != null ? course.getLanguage().getLabel() : null)
            .status(course.getStatus())
            .totalVideoDurationSeconds(course.getTotalVideoDurationSeconds())
            .lectureCount(course.getLectureCount())
            .studentCount(course.getStudentCount())
            .averageRating(course.getAverageRating())
            .rejectionReason(course.getRejectionReason())
            .publishedAt(course.getPublishedAt())
            .createdAt(course.getCreatedAt())
            .updatedAt(course.getUpdatedAt())
            .sections(course.getSections() != null ? course.getSections().stream()
                .map(CourseSectionResponse::from)
                .collect(Collectors.toList()) : List.of())
            .build();
    }
}
