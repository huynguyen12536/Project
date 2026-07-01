package com.learnhub.catalog.course.service;

import com.learnhub.catalog.course.dto.request.CreateCourseLectureRequest;
import com.learnhub.catalog.course.dto.request.CreateCourseRequest;
import com.learnhub.catalog.course.dto.request.CreateCourseSectionRequest;
import com.learnhub.catalog.course.dto.request.RejectCourseRequest;
import com.learnhub.catalog.course.dto.request.UpdateCourseLectureRequest;
import com.learnhub.catalog.course.dto.request.UpdateCourseRequest;
import com.learnhub.catalog.course.dto.request.UpdateCourseSectionRequest;
import com.learnhub.catalog.course.dto.response.CourseLectureResponse;
import com.learnhub.catalog.course.dto.response.CourseResponse;
import com.learnhub.catalog.course.dto.response.CourseSectionResponse;
import com.learnhub.catalog.course.model.Course;
import com.learnhub.catalog.course.model.Course.CourseStatus;
import com.learnhub.catalog.course.model.CourseLecture;
import com.learnhub.catalog.course.model.CourseSection;
import com.learnhub.catalog.course.repository.CourseLectureRepository;
import com.learnhub.catalog.course.repository.CourseRepository;
import com.learnhub.catalog.course.repository.CourseSectionRepository;
import com.learnhub.catalog.taxonomy.model.CourseCategory;
import com.learnhub.catalog.taxonomy.model.CourseLanguage;
import com.learnhub.catalog.taxonomy.model.CourseLevel;
import com.learnhub.catalog.taxonomy.model.CourseSubcategory;
import com.learnhub.catalog.taxonomy.repository.CourseCategoryRepository;
import com.learnhub.catalog.taxonomy.repository.CourseLanguageRepository;
import com.learnhub.catalog.taxonomy.repository.CourseLevelRepository;
import com.learnhub.catalog.taxonomy.repository.CourseSubcategoryRepository;
import com.learnhub.common.exception.ResourceNotFoundException;
import com.learnhub.user.model.User;
import com.learnhub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseService {
    private final CourseRepository courseRepository;
    private final CourseSectionRepository courseSectionRepository;
    private final CourseLectureRepository courseLectureRepository;
    private final UserRepository userRepository;
    private final CourseCategoryRepository categoryRepository;
    private final CourseSubcategoryRepository subcategoryRepository;
    private final CourseLevelRepository levelRepository;
    private final CourseLanguageRepository languageRepository;

    @Transactional(readOnly = true)
    public List<CourseResponse> getInstructorCourses(UUID instructorId) {
        return courseRepository.findByInstructorIdOrderByUpdatedAtDesc(instructorId).stream()
            .map(CourseResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public CourseResponse getInstructorCourse(UUID courseId, UUID instructorId) {
        return CourseResponse.from(getOwnedCourse(courseId, instructorId));
    }

    @Transactional(readOnly = true)
    public List<CourseSectionResponse> getCourseSections(UUID courseId, UUID instructorId) {
        getOwnedCourse(courseId, instructorId);
        return courseSectionRepository.findByCourseIdOrderByDisplayOrderAsc(courseId).stream()
            .map(CourseSectionResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> getPendingCoursesForAdmin() {
        return courseRepository.findAllByStatus(CourseStatus.PENDING_REVIEW).stream()
            .map(CourseResponse::from)
            .toList();
    }

    public CourseResponse createCourse(CreateCourseRequest request, Authentication authentication) {
        UUID instructorId = UUID.fromString(authentication.getName());
        User instructor = userRepository.findById(instructorId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + instructorId));

        CourseCategory category = request.categoryId() != null ? categoryRepository.findById(request.categoryId()).orElse(null) : null;
        CourseSubcategory subcategory = request.subcategoryId() != null ? subcategoryRepository.findById(request.subcategoryId()).orElse(null) : null;
        CourseLevel level = request.levelId() != null ? levelRepository.findById(request.levelId()).orElse(null) : null;
        CourseLanguage language = request.languageId() != null ? languageRepository.findById(request.languageId()).orElse(null) : null;

        Course course = Course.builder()
            .instructor(instructor)
            .title(request.title())
            .subtitle(request.subtitle())
            .description(request.description())
            .thumbnailUrl(request.thumbnailUrl())
            .promoVideoUrl(request.promoVideoUrl())
            .category(category)
            .subcategory(subcategory)
            .level(level)
            .language(language)
            .status(CourseStatus.DRAFT)
            .totalVideoDurationSeconds(0)
            .lectureCount(0)
            .studentCount(0)
            .build();

        return CourseResponse.from(courseRepository.save(course));
    }

    public CourseResponse updateCourse(UUID courseId, UpdateCourseRequest request, UUID instructorId) {
        Course course = getOwnedCourse(courseId, instructorId);

        if (request.title() != null) course.setTitle(request.title());
        if (request.subtitle() != null) course.setSubtitle(request.subtitle());
        if (request.description() != null) course.setDescription(request.description());
        if (request.thumbnailUrl() != null) course.setThumbnailUrl(request.thumbnailUrl());
        if (request.promoVideoUrl() != null) course.setPromoVideoUrl(request.promoVideoUrl());
        if (request.categoryId() != null) {
            course.setCategory(categoryRepository.findById(request.categoryId()).orElse(null));
        }
        if (request.subcategoryId() != null) {
            course.setSubcategory(subcategoryRepository.findById(request.subcategoryId()).orElse(null));
        }
        if (request.levelId() != null) {
            course.setLevel(levelRepository.findById(request.levelId()).orElse(null));
        }
        if (request.languageId() != null) {
            course.setLanguage(languageRepository.findById(request.languageId()).orElse(null));
        }

        return CourseResponse.from(courseRepository.save(course));
    }

    public CourseResponse submitForReview(UUID courseId, UUID instructorId) {
        Course course = getOwnedCourse(courseId, instructorId);
        validateCourseReadyForReview(course);
        course.setStatus(CourseStatus.PENDING_REVIEW);
        return CourseResponse.from(courseRepository.save(course));
    }

    public CourseResponse approveCourse(UUID courseId) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));
        if (course.getStatus() != CourseStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Course must be in PENDING_REVIEW to be approved");
        }
        course.setStatus(CourseStatus.PUBLISHED);
        course.setPublishedAt(java.time.LocalDateTime.now());
        return CourseResponse.from(courseRepository.save(course));
    }

    public CourseResponse rejectCourse(UUID courseId, RejectCourseRequest request) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));
        if (course.getStatus() != CourseStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Course must be in PENDING_REVIEW to be rejected");
        }
        course.setStatus(CourseStatus.REJECTED);
        course.setRejectionReason(request.rejectionReason());
        return CourseResponse.from(courseRepository.save(course));
    }

    public CourseSectionResponse createSection(UUID courseId, CreateCourseSectionRequest request, UUID instructorId) {
        Course course = getOwnedCourse(courseId, instructorId);
        
        Integer order = request.displayOrder();
        if (order == null) {
            order = courseSectionRepository.findByCourseIdOrderByDisplayOrderAsc(courseId).size() * 10 + 10;
        }
        
        CourseSection section = CourseSection.builder()
            .course(course)
            .title(request.title())
            .displayOrder(order)
            .build();
        
        return CourseSectionResponse.from(courseSectionRepository.save(section));
    }

    public CourseSectionResponse updateSection(UUID courseId, UUID sectionId, UpdateCourseSectionRequest request, UUID instructorId) {
        CourseSection section = getOwnedSection(courseId, sectionId, instructorId);
        
        if (request.title() != null) section.setTitle(request.title());
        if (request.displayOrder() != null) section.setDisplayOrder(request.displayOrder());
        
        return CourseSectionResponse.from(courseSectionRepository.save(section));
    }

    public void deleteSection(UUID courseId, UUID sectionId, UUID instructorId) {
        Course course = getOwnedCourse(courseId, instructorId);
        CourseSection section = getOwnedSection(courseId, sectionId, instructorId);
        courseSectionRepository.delete(section);
        updateCourseStats(course);
    }

    public CourseLectureResponse createLecture(UUID courseId, UUID sectionId, CreateCourseLectureRequest request, UUID instructorId) {
        Course course = getOwnedCourse(courseId, instructorId);
        CourseSection section = getOwnedSection(courseId, sectionId, instructorId);
        
        Integer order = request.displayOrder();
        if (order == null) {
            order = courseLectureRepository.findBySectionIdOrderByDisplayOrderAsc(sectionId).size() * 10 +10;
        }
        
        CourseLecture lecture = CourseLecture.builder()
            .course(course)
            .section(section)
            .title(request.title())
            .type(request.type())
            .content(request.content())
            .videoUrl(request.videoUrl())
            .durationSeconds(request.durationSeconds())
            .displayOrder(order)
            .isFreePreview(request.isFreePreview() != null ? request.isFreePreview() : false)
            .build();
        
        CourseLecture savedLecture = courseLectureRepository.save(lecture);
        
        updateCourseStats(course);
        
        return CourseLectureResponse.from(savedLecture);
    }

    public CourseLectureResponse updateLecture(UUID courseId, UUID sectionId, UUID lectureId, UpdateCourseLectureRequest request, UUID instructorId) {
        CourseLecture lecture = getOwnedLecture(courseId, sectionId, lectureId, instructorId);
        
        if (request.title() != null) lecture.setTitle(request.title());
        if (request.type() != null) lecture.setType(request.type());
        if (request.content() != null) lecture.setContent(request.content());
        if (request.videoUrl() != null) lecture.setVideoUrl(request.videoUrl());
        if (request.durationSeconds() != null) lecture.setDurationSeconds(request.durationSeconds());
        if (request.displayOrder() != null) lecture.setDisplayOrder(request.displayOrder());
        if (request.isFreePreview() != null) lecture.setIsFreePreview(request.isFreePreview());
        
        CourseLecture savedLecture = courseLectureRepository.save(lecture);
        updateCourseStats(lecture.getCourse());
        
        return CourseLectureResponse.from(savedLecture);
    }

    public void deleteLecture(UUID courseId, UUID sectionId, UUID lectureId, UUID instructorId) {
        Course course = getOwnedCourse(courseId, instructorId);
        CourseLecture lecture = getOwnedLecture(courseId, sectionId, lectureId, instructorId);
        courseLectureRepository.delete(lecture);
        updateCourseStats(course);
    }

    private Course getOwnedCourse(UUID courseId, UUID instructorId) {
        return courseRepository.findByIdAndInstructorId(courseId, instructorId)
            .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));
    }

    private CourseSection getOwnedSection(UUID courseId, UUID sectionId, UUID instructorId) {
        getOwnedCourse(courseId, instructorId);
        CourseSection section = courseSectionRepository.findById(sectionId)
            .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + sectionId));
        if (!section.getCourse().getId().equals(courseId)) {
            throw new ResourceNotFoundException("Section not found in course: " + sectionId);
        }
        return section;
    }

    private CourseLecture getOwnedLecture(UUID courseId, UUID sectionId, UUID lectureId, UUID instructorId) {
        CourseSection section = getOwnedSection(courseId, sectionId, instructorId);
        CourseLecture lecture = courseLectureRepository.findById(lectureId)
            .orElseThrow(() -> new ResourceNotFoundException("Lecture not found: " + lectureId));
        if (!lecture.getCourse().getId().equals(courseId) || !lecture.getSection().getId().equals(section.getId())) {
            throw new ResourceNotFoundException("Lecture not found in section: " + lectureId);
        }
        return lecture;
    }

    private void validateCourseReadyForReview(Course course) {
        if (course.getTitle() == null || course.getTitle().isBlank()) {
            throw new IllegalStateException("Course must have a title");
        }
        if (course.getThumbnailUrl() == null || course.getThumbnailUrl().isBlank()) {
            throw new IllegalStateException("Course must have a thumbnail");
        }
        if (course.getCategory() == null) {
            throw new IllegalStateException("Course must have a category");
        }
        if (course.getLectureCount() < 5) {
            throw new IllegalStateException("Course must have at least 5 lectures");
        }
        if (course.getTotalVideoDurationSeconds() < 1800) { // 30 minutes minimum
            throw new IllegalStateException("Course must have at least 30 minutes of video content");
        }
    }

    private void updateCourseStats(Course course) {
        List<CourseSection> sections = courseSectionRepository.findByCourseIdOrderByDisplayOrderAsc(course.getId());
        
        int lectureCount = 0;
        int totalSeconds = 0;
        
        for (CourseSection section : sections) {
            List<CourseLecture> lectures = courseLectureRepository.findBySectionIdOrderByDisplayOrderAsc(section.getId());
            lectureCount += lectures.size();
            for (CourseLecture lecture : lectures) {
                if (lecture.getDurationSeconds() != null) {
                    totalSeconds += lecture.getDurationSeconds();
                }
            }
        }
        
        course.setLectureCount(lectureCount);
        course.setTotalVideoDurationSeconds(totalSeconds);
        courseRepository.save(course);
    }
}
