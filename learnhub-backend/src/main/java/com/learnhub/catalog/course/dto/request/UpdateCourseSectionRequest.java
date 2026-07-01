package com.learnhub.catalog.course.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UpdateCourseSectionRequest(
    @Size(max = 255, message = "Ten chuong khong qua 255 ky tu")
    String title,

    Integer displayOrder
) {}
