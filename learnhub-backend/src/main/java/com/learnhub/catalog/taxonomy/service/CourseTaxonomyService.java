package com.learnhub.catalog.taxonomy.service;

import com.learnhub.catalog.taxonomy.dto.request.*;
import com.learnhub.catalog.taxonomy.dto.response.*;
import com.learnhub.catalog.taxonomy.model.*;
import com.learnhub.catalog.taxonomy.repository.*;
import com.learnhub.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseTaxonomyService {

    private final CourseCategoryRepository categoryRepository;
    private final CourseSubcategoryRepository subcategoryRepository;
    private final CourseLevelRepository levelRepository;
    private final CourseLanguageRepository languageRepository;
    private final CourseTagRepository tagRepository;

    @Transactional(readOnly = true)
    public CourseTaxonomyBundleResponse getAdminBundle() {
        return CourseTaxonomyBundleResponse.builder()
            .categories(categoryRepository.findAllByOrderByDisplayOrderAscNameAsc().stream().map(CourseCategoryResponse::from).toList())
            .subcategories(subcategoryRepository.findAllByOrderByDisplayOrderAscNameAsc().stream().map(CourseSubcategoryResponse::from).toList())
            .levels(levelRepository.findAllByOrderByDisplayOrderAscLabelAsc().stream().map(CourseLevelResponse::from).toList())
            .languages(languageRepository.findAllByOrderByDisplayOrderAscLabelAsc().stream().map(CourseLanguageResponse::from).toList())
            .tags(tagRepository.findAllByOrderByDisplayOrderAscNameAsc().stream().map(CourseTagResponse::from).toList())
            .build();
    }

    @Transactional(readOnly = true)
    public CourseTaxonomyBundleResponse getActiveBundle() {
        return CourseTaxonomyBundleResponse.builder()
            .categories(categoryRepository.findByIsActiveTrueOrderByDisplayOrderAscNameAsc().stream().map(CourseCategoryResponse::from).toList())
            .subcategories(subcategoryRepository.findByIsActiveTrueOrderByDisplayOrderAscNameAsc().stream().map(CourseSubcategoryResponse::from).toList())
            .levels(levelRepository.findByIsActiveTrueOrderByDisplayOrderAscLabelAsc().stream().map(CourseLevelResponse::from).toList())
            .languages(languageRepository.findByIsActiveTrueOrderByDisplayOrderAscLabelAsc().stream().map(CourseLanguageResponse::from).toList())
            .tags(tagRepository.findByIsActiveTrueOrderByDisplayOrderAscNameAsc().stream().map(CourseTagResponse::from).toList())
            .build();
    }

    public CourseCategoryResponse createCategory(CreateCourseCategoryRequest request) {
        String normalizedName = normalizeText(request.getName());
        if (categoryRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new IllegalArgumentException("Danh muc da ton tai: " + normalizedName);
        }

        CourseCategory category = CourseCategory.builder()
            .name(normalizedName)
            .slug(ensureUniqueSlug(baseSlug(normalizedName), categoryRepository::existsBySlug))
            .description(normalizeOptionalText(request.getDescription()))
            .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : nextOrder(categoryRepository.findAllByOrderByDisplayOrderAscNameAsc().stream().map(CourseCategory::getDisplayOrder).toList()))
            .isActive(true)
            .build();

        return CourseCategoryResponse.from(categoryRepository.save(category));
    }

    public CourseSubcategoryResponse createSubcategory(CreateCourseSubcategoryRequest request) {
        CourseCategory category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));

        String normalizedName = normalizeText(request.getName());
        if (subcategoryRepository.existsByCategoryAndNameIgnoreCase(category, normalizedName)) {
            throw new IllegalArgumentException("Phan loai con da ton tai trong danh muc nay: " + normalizedName);
        }

        CourseSubcategory subcategory = CourseSubcategory.builder()
            .category(category)
            .name(normalizedName)
            .slug(ensureUniqueSlug(baseSlug(normalizedName), subcategoryRepository::existsBySlug))
            .description(normalizeOptionalText(request.getDescription()))
            .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : nextOrder(subcategoryRepository.findAllByOrderByDisplayOrderAscNameAsc().stream().map(CourseSubcategory::getDisplayOrder).toList()))
            .isActive(true)
            .build();

        return CourseSubcategoryResponse.from(subcategoryRepository.save(subcategory));
    }

    public CourseLevelResponse createLevel(CreateCourseLevelRequest request) {
        String normalizedLabel = normalizeText(request.getLabel());
        String code = baseSlug(normalizedLabel);

        if (levelRepository.existsByLabelIgnoreCase(normalizedLabel) || levelRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("Trinh do da ton tai: " + normalizedLabel);
        }

        CourseLevel level = CourseLevel.builder()
            .code(code)
            .label(normalizedLabel)
            .description(normalizeOptionalText(request.getDescription()))
            .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : nextOrder(levelRepository.findAllByOrderByDisplayOrderAscLabelAsc().stream().map(CourseLevel::getDisplayOrder).toList()))
            .isActive(true)
            .build();

        return CourseLevelResponse.from(levelRepository.save(level));
    }

    public CourseLanguageResponse createLanguage(CreateCourseLanguageRequest request) {
        String normalizedLabel = normalizeText(request.getLabel());
        String code = baseSlug(normalizedLabel);

        if (languageRepository.existsByLabelIgnoreCase(normalizedLabel) || languageRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("Ngon ngu da ton tai: " + normalizedLabel);
        }

        CourseLanguage language = CourseLanguage.builder()
            .code(code)
            .label(normalizedLabel)
            .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : nextOrder(languageRepository.findAllByOrderByDisplayOrderAscLabelAsc().stream().map(CourseLanguage::getDisplayOrder).toList()))
            .isActive(true)
            .build();

        return CourseLanguageResponse.from(languageRepository.save(language));
    }

    public CourseTagResponse createTag(CreateCourseTagRequest request) {
        String normalizedName = normalizeText(request.getName());
        String slug = ensureUniqueSlug(baseSlug(normalizedName), tagRepository::existsBySlug);

        if (tagRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new IllegalArgumentException("Tag da ton tai: " + normalizedName);
        }

        CourseTag tag = CourseTag.builder()
            .name(normalizedName)
            .slug(slug)
            .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : nextOrder(tagRepository.findAllByOrderByDisplayOrderAscNameAsc().stream().map(CourseTag::getDisplayOrder).toList()))
            .isActive(true)
            .build();

        return CourseTagResponse.from(tagRepository.save(tag));
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeOptionalText(String value) {
        String normalized = normalizeText(value);
        return normalized.isEmpty() ? null : normalized;
    }

    private int nextOrder(List<Integer> orders) {
        return orders.stream().filter(order -> order != null).max(Integer::compareTo).orElse(0) + 10;
    }

    private String ensureUniqueSlug(String baseSlug, SlugExistenceChecker checker) {
        String candidate = baseSlug;
        int suffix = 2;
        while (checker.exists(candidate)) {
            candidate = baseSlug + "-" + suffix++;
        }
        return candidate;
    }

    private String baseSlug(String value) {
        String ascii = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-|-$)", "")
            .replaceAll("-{2,}", "-");

        return ascii.isBlank() ? "item" : ascii;
    }

    @FunctionalInterface
    private interface SlugExistenceChecker {
        boolean exists(String slug);
    }
}
