package com.learnhub.catalog.course.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record RejectCourseRequest(
    @NotBlank(message = "Ly do tu choi khong duoc de trong")
    String rejectionReason
) {}
