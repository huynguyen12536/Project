package com.learnhub.catalog.course.controller;

import com.learnhub.catalog.course.dto.request.CreateCourseLectureRequest;
import com.learnhub.catalog.course.dto.request.CreateCourseRequest;
import com.learnhub.catalog.course.dto.request.CreateCourseSectionRequest;
import com.learnhub.catalog.course.dto.request.UpdateCourseLectureRequest;
import com.learnhub.catalog.course.dto.request.UpdateCourseRequest;
import com.learnhub.catalog.course.dto.request.UpdateCourseSectionRequest;
import com.learnhub.catalog.course.dto.response.CourseLectureResponse;
import com.learnhub.catalog.course.dto.response.CourseResponse;
import com.learnhub.catalog.course.dto.response.CourseSectionResponse;
import com.learnhub.catalog.course.service.CourseService;
import com.learnhub.common.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/instructor/courses", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Instructor Courses", description = "APIs for instructors to manage courses")
public class InstructorCourseController {
    private final CourseService courseService;

    @Operation(summary = "Get all instructor courses")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved courses"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<CourseResponse>> getCourses(Authentication authentication) {
        UUID instructorId = getCurrentUserId(authentication);
        List<CourseResponse> courses = courseService.getInstructorCourses(instructorId);
        return ResponseEntity.ok(courses);
    }

    @Operation(summary = "Get a specific instructor course")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved course"),
        @ApiResponse(responseCode = "404", description = "Course not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{courseId}")
    public ResponseEntity<CourseResponse> getCourse(
        @PathVariable("courseId") UUID courseId,
        Authentication authentication
    ) {
        UUID instructorId = getCurrentUserId(authentication);
        CourseResponse course = courseService.getInstructorCourse(courseId, instructorId);
        return ResponseEntity.ok(course);
    }

    @Operation(summary = "Create a new course")
    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(
        @Valid @RequestBody CreateCourseRequest request,
        Authentication authentication
    ) {
        CourseResponse course = courseService.createCourse(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(course);
    }

    @Operation(summary = "Update an existing course")
    @PutMapping("/{courseId}")
    public ResponseEntity<CourseResponse> updateCourse(
        @PathVariable("courseId") UUID courseId,
        @Valid @RequestBody UpdateCourseRequest request,
        Authentication authentication
    ) {
        UUID instructorId = getCurrentUserId(authentication);
        CourseResponse course = courseService.updateCourse(courseId, request, instructorId);
        return ResponseEntity.ok(course);
    }

    @Operation(summary = "Submit course for admin review")
    @PostMapping("/{courseId}/submit-review")
    public ResponseEntity<CourseResponse> submitForReview(
        @PathVariable("courseId") UUID courseId,
        Authentication authentication
    ) {
        UUID instructorId = getCurrentUserId(authentication);
        CourseResponse course = courseService.submitForReview(courseId, instructorId);
        return ResponseEntity.ok(course);
    }

    @Operation(summary = "Get sections of a course")
    @GetMapping("/{courseId}/sections")
    public ResponseEntity<List<CourseSectionResponse>> getSections(
        @PathVariable("courseId") UUID courseId,
        Authentication authentication
    ) {
        UUID instructorId = getCurrentUserId(authentication);
        List<CourseSectionResponse> sections = courseService.getCourseSections(courseId, instructorId);
        return ResponseEntity.ok(sections);
    }

    @Operation(summary = "Create a new course section")
    @PostMapping("/{courseId}/sections")
    public ResponseEntity<CourseSectionResponse> createSection(
        @PathVariable("courseId") UUID courseId,
        @Valid @RequestBody CreateCourseSectionRequest request,
        Authentication authentication
    ) {
        UUID instructorId = getCurrentUserId(authentication);
        CourseSectionResponse section = courseService.createSection(courseId, request, instructorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(section);
    }

    @Operation(summary = "Update an existing course section")
    @PutMapping("/{courseId}/sections/{sectionId}")
    public ResponseEntity<CourseSectionResponse> updateSection(
        @PathVariable("courseId") UUID courseId,
        @PathVariable("sectionId") UUID sectionId,
        @Valid @RequestBody UpdateCourseSectionRequest request,
        Authentication authentication
    ) {
        UUID instructorId = getCurrentUserId(authentication);
        CourseSectionResponse section = courseService.updateSection(courseId, sectionId, request, instructorId);
        return ResponseEntity.ok(section);
    }

    @Operation(summary = "Delete a course section")
    @DeleteMapping("/{courseId}/sections/{sectionId}")
    public ResponseEntity<Void> deleteSection(
        @PathVariable("courseId") UUID courseId,
        @PathVariable("sectionId") UUID sectionId,
        Authentication authentication
    ) {
        UUID instructorId = getCurrentUserId(authentication);
        courseService.deleteSection(courseId, sectionId, instructorId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Create a new lecture in a section")
    @PostMapping("/{courseId}/sections/{sectionId}/lectures")
    public ResponseEntity<CourseLectureResponse> createLecture(
        @PathVariable("courseId") UUID courseId,
        @PathVariable("sectionId") UUID sectionId,
        @Valid @RequestBody CreateCourseLectureRequest request,
        Authentication authentication
    ) {
        UUID instructorId = getCurrentUserId(authentication);
        CourseLectureResponse lecture = courseService.createLecture(courseId, sectionId, request, instructorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(lecture);
    }

    @Operation(summary = "Update an existing lecture")
    @PutMapping("/{courseId}/sections/{sectionId}/lectures/{lectureId}")
    public ResponseEntity<CourseLectureResponse> updateLecture(
        @PathVariable("courseId") UUID courseId,
        @PathVariable("sectionId") UUID sectionId,
        @PathVariable("lectureId") UUID lectureId,
        @Valid @RequestBody UpdateCourseLectureRequest request,
        Authentication authentication
    ) {
        UUID instructorId = getCurrentUserId(authentication);
        CourseLectureResponse lecture = courseService.updateLecture(courseId, sectionId, lectureId, request, instructorId);
        return ResponseEntity.ok(lecture);
    }

    @Operation(summary = "Delete a lecture")
    @DeleteMapping("/{courseId}/sections/{sectionId}/lectures/{lectureId}")
    public ResponseEntity<Void> deleteLecture(
        @PathVariable("courseId") UUID courseId,
        @PathVariable("sectionId") UUID sectionId,
        @PathVariable("lectureId") UUID lectureId,
        Authentication authentication
    ) {
        UUID instructorId = getCurrentUserId(authentication);
        courseService.deleteLecture(courseId, sectionId, lectureId, instructorId);
        return ResponseEntity.noContent().build();
    }

    private UUID getCurrentUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
