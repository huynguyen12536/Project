package com.learnhub.catalog.taxonomy.controller;

import com.learnhub.catalog.taxonomy.dto.response.CourseTaxonomyBundleResponse;
import com.learnhub.catalog.taxonomy.service.CourseTaxonomyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/taxonomy")
@RequiredArgsConstructor
public class CourseTaxonomyController {

    private final CourseTaxonomyService courseTaxonomyService;

    @GetMapping("/options")
    public ResponseEntity<CourseTaxonomyBundleResponse> getActiveTaxonomy() {
        return ResponseEntity.ok(courseTaxonomyService.getActiveBundle());
    }
}
