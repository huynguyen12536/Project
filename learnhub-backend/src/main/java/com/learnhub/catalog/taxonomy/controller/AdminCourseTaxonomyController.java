package com.learnhub.catalog.taxonomy.controller;

import com.learnhub.catalog.taxonomy.dto.request.*;
import com.learnhub.catalog.taxonomy.dto.response.*;
import com.learnhub.catalog.taxonomy.service.CourseTaxonomyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/taxonomy")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCourseTaxonomyController {

    private final CourseTaxonomyService courseTaxonomyService;

    @GetMapping
    public ResponseEntity<CourseTaxonomyBundleResponse> getAdminTaxonomy() {
        return ResponseEntity.ok(courseTaxonomyService.getAdminBundle());
    }

    @PostMapping("/categories")
    public ResponseEntity<CourseCategoryResponse> createCategory(@Valid @RequestBody CreateCourseCategoryRequest request) {
        return ResponseEntity.ok(courseTaxonomyService.createCategory(request));
    }

    @PostMapping("/subcategories")
    public ResponseEntity<CourseSubcategoryResponse> createSubcategory(@Valid @RequestBody CreateCourseSubcategoryRequest request) {
        return ResponseEntity.ok(courseTaxonomyService.createSubcategory(request));
    }

    @PostMapping("/levels")
    public ResponseEntity<CourseLevelResponse> createLevel(@Valid @RequestBody CreateCourseLevelRequest request) {
        return ResponseEntity.ok(courseTaxonomyService.createLevel(request));
    }

    @PostMapping("/languages")
    public ResponseEntity<CourseLanguageResponse> createLanguage(@Valid @RequestBody CreateCourseLanguageRequest request) {
        return ResponseEntity.ok(courseTaxonomyService.createLanguage(request));
    }

    @PostMapping("/tags")
    public ResponseEntity<CourseTagResponse> createTag(@Valid @RequestBody CreateCourseTagRequest request) {
        return ResponseEntity.ok(courseTaxonomyService.createTag(request));
    }
}
