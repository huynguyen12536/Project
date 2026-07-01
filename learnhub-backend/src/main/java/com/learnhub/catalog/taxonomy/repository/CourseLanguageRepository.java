package com.learnhub.catalog.taxonomy.repository;

import com.learnhub.catalog.taxonomy.model.CourseLanguage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourseLanguageRepository extends JpaRepository<CourseLanguage, UUID> {
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByLabelIgnoreCase(String label);
    List<CourseLanguage> findAllByOrderByDisplayOrderAscLabelAsc();
    List<CourseLanguage> findByIsActiveTrueOrderByDisplayOrderAscLabelAsc();
}
