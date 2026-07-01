package com.learnhub.catalog.taxonomy.repository;

import com.learnhub.catalog.taxonomy.model.CourseTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourseTagRepository extends JpaRepository<CourseTag, UUID> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsBySlug(String slug);
    List<CourseTag> findAllByOrderByDisplayOrderAscNameAsc();
    List<CourseTag> findByIsActiveTrueOrderByDisplayOrderAscNameAsc();
}
