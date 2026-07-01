package com.learnhub.catalog.course.repository;

import com.learnhub.catalog.course.model.CourseLecture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourseLectureRepository extends JpaRepository<CourseLecture, UUID> {
    List<CourseLecture> findBySectionIdOrderByDisplayOrderAsc(UUID sectionId);
    List<CourseLecture> findByCourseIdOrderBySectionDisplayOrderAscDisplayOrderAsc(UUID courseId);
}
