package com.learnhub.catalog.course.controller;

import com.learnhub.catalog.course.dto.request.RejectCourseRequest;
import com.learnhub.catalog.course.dto.response.CourseResponse;
import com.learnhub.catalog.course.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/courses")
@RequiredArgsConstructor
@Tag(name = "Admin Courses", description = "Admin APIs for reviewing and managing courses")
public class AdminCourseController {
    private final CourseService courseService;

    @Operation(summary = "Get all pending courses for review")
    @GetMapping("/pending")
    public ResponseEntity<List<CourseResponse>> getPendingCourses() {
        List<CourseResponse> pendingCourses = courseService.getPendingCoursesForAdmin();
        return ResponseEntity.ok(pendingCourses);
    }

    @Operation(summary = "Approve a course for publishing")
    @PostMapping("/{courseId}/approve")
    public ResponseEntity<CourseResponse> approveCourse(@PathVariable UUID courseId) {
        CourseResponse course = courseService.approveCourse(courseId);
        return ResponseEntity.ok(course);
    }

    @Operation(summary = "Reject a course with feedback")
    @PostMapping("/{courseId}/reject")
    public ResponseEntity<CourseResponse> rejectCourse(
            @PathVariable UUID courseId,
            @Valid @RequestBody RejectCourseRequest request
    ) {
        CourseResponse course = courseService.rejectCourse(courseId, request);
        return ResponseEntity.ok(course);
    }

    @Operation(summary = "Get a specific course by ID")
    @GetMapping("/{courseId}")
    public ResponseEntity<CourseResponse> getCourse(@PathVariable UUID courseId) {
        return ResponseEntity.ok(courseService.getPendingCoursesForAdmin().stream()
                .filter(c -> c.id().equals(courseId))
                .findFirst()
                .orElseThrow(() -> new com.learnhub.common.exception.ResourceNotFoundException("Course not found: " + courseId)));
    }
}
