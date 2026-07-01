package com.learnhub.catalog.course.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CreateCourseSectionRequest(
    @NotBlank(message = "Ten chuong khong duoc de trong")
    @Size(max = 255, message = "Ten chuong khong qua 255 ky tu")
    String title,

    Integer displayOrder
) {}
