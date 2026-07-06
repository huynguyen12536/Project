package com.learnhub.catalog.course.controller;

import com.learnhub.catalog.course.dto.request.RejectCourseRequest;
import com.learnhub.catalog.course.dto.response.CourseDetailResponse;
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

    @Operation(summary = "Get all courses for admin management")
    @GetMapping
    public ResponseEntity<List<CourseResponse>> getAllCourses() {
        List<CourseResponse> allCourses = courseService.getAllCoursesForAdmin();
        return ResponseEntity.ok(allCourses);
    }

    @Operation(summary = "Get all pending courses for review")
    @GetMapping("/pending")
    public ResponseEntity<List<CourseResponse>> getPendingCourses() {
        List<CourseResponse> pendingCourses = courseService.getPendingCoursesForAdmin();
        return ResponseEntity.ok(pendingCourses);
    }

    @Operation(summary = "Approve a course for publishing")
    @PostMapping("/{courseId}/approve")
    public ResponseEntity<CourseResponse> approveCourse(@PathVariable("courseId") UUID courseId) {
        CourseResponse course = courseService.approveCourse(courseId);
        return ResponseEntity.ok(course);
    }

    @Operation(summary = "Reject a course with feedback")
    @PostMapping("/{courseId}/reject")
    public ResponseEntity<CourseResponse> rejectCourse(
            @PathVariable("courseId") UUID courseId,
            @Valid @RequestBody RejectCourseRequest request
    ) {
        CourseResponse course = courseService.rejectCourse(courseId, request);
        return ResponseEntity.ok(course);
    }

    @Operation(summary = "Get a specific course by ID for review")
    @GetMapping("/{courseId}")
    public ResponseEntity<CourseResponse> getCourse(@PathVariable("courseId") UUID courseId) {
        CourseResponse course = courseService.getAdminCourse(courseId);
        return ResponseEntity.ok(course);
    }

    @Operation(summary = "Get course details for admin management")
    @GetMapping("/{courseId}/details")
    public ResponseEntity<CourseDetailResponse> getCourseDetails(@PathVariable("courseId") UUID courseId) {
        CourseDetailResponse course = courseService.getCourseDetailForAdminWithSections(courseId);
        return ResponseEntity.ok(course);
    }

    @Operation(summary = "Delete a course")
    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> deleteCourse(@PathVariable("courseId") UUID courseId) {
        courseService.deleteCourse(courseId);
        return ResponseEntity.noContent().build();
    }
}
