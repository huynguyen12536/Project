package com.learnhub.catalog.course.dto.request;

import com.learnhub.catalog.course.model.CourseLecture;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CreateCourseLectureRequest(
    @NotBlank(message = "Ten bai giang khong duoc de trong")
    @Size(max = 255, message = "Ten bai giang khong qua 255 ky tu")
    String title,

    @NotNull(message = "Loai bai giang khong duoc de trong")
    CourseLecture.LectureType type,

    String content,
    String videoUrl,
    Integer durationSeconds,
    Integer displayOrder,
    Boolean isFreePreview
) {}
