package com.learnhub.catalog.taxonomy.repository;

import com.learnhub.catalog.taxonomy.model.CourseCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseCategoryRepository extends JpaRepository<CourseCategory, UUID> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsBySlug(String slug);
    Optional<CourseCategory> findBySlug(String slug);
    List<CourseCategory> findAllByOrderByDisplayOrderAscNameAsc();
    List<CourseCategory> findByIsActiveTrueOrderByDisplayOrderAscNameAsc();
}
