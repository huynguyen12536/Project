package com.learnhub.catalog.taxonomy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCourseTagRequest {

    @NotBlank
    @Size(max = 120)
    private String name;

    private Integer displayOrder;
}
