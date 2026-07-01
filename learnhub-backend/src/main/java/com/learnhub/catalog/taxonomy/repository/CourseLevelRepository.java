package com.learnhub.catalog.taxonomy.repository;

import com.learnhub.catalog.taxonomy.model.CourseLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourseLevelRepository extends JpaRepository<CourseLevel, UUID> {
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByLabelIgnoreCase(String label);
    List<CourseLevel> findAllByOrderByDisplayOrderAscLabelAsc();
    List<CourseLevel> findByIsActiveTrueOrderByDisplayOrderAscLabelAsc();
}
