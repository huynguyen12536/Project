package com.learnhub.catalog.taxonomy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCourseLevelRequest {

    @NotBlank
    @Size(max = 120)
    private String label;

    @Size(max = 500)
    private String description;

    private Integer displayOrder;
}
