package com.learnhub.catalog.course.dto.request;

import com.learnhub.catalog.course.model.CourseLecture;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UpdateCourseLectureRequest(
    @Size(max = 255, message = "Ten bai giang khong qua 255 ky tu")
    String title,

    CourseLecture.LectureType type,
    String content,
    String videoUrl,
    Integer durationSeconds,
    Integer displayOrder,
    Boolean isFreePreview
) {}
