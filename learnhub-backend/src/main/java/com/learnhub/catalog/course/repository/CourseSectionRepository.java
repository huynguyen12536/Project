package com.learnhub.catalog.course.repository;

import com.learnhub.catalog.course.model.CourseSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourseSectionRepository extends JpaRepository<CourseSection, UUID> {
    List<CourseSection> findByCourseIdOrderByDisplayOrderAsc(UUID courseId);
}
