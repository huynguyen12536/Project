package com.learnhub.catalog.taxonomy.repository;

import com.learnhub.catalog.taxonomy.model.CourseCategory;
import com.learnhub.catalog.taxonomy.model.CourseSubcategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourseSubcategoryRepository extends JpaRepository<CourseSubcategory, UUID> {
    boolean existsByCategoryAndNameIgnoreCase(CourseCategory category, String name);
    boolean existsBySlug(String slug);
    List<CourseSubcategory> findAllByOrderByDisplayOrderAscNameAsc();
    List<CourseSubcategory> findByIsActiveTrueOrderByDisplayOrderAscNameAsc();
}
