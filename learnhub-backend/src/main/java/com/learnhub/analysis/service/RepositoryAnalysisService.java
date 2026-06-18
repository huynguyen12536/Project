package com.learnhub.analysis.service;

import com.learnhub.analysis.dto.AnalysisResult;
import com.learnhub.analysis.dto.LanguageDetectionResult;
import com.learnhub.analysis.dto.SkillScore;
import com.learnhub.github.entity.RepositorySnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Service responsible for analyzing GitHub repository snapshots.
 * Performs language detection and skill extraction on repository metadata.
 *
 * This service is read-only and performs no persistence operations.
 * Analysis results are returned as DTOs without database storage.
 */
@Service
@Transactional(readOnly = true)
@Slf4j
public class RepositoryAnalysisService {

    private final LanguageDetectionEngine languageDetectionEngine;
    private final SkillExtractionEngine skillExtractionEngine;

    /**
     * Constructs the service with required engine dependencies.
     *
     * @param languageDetectionEngine Engine for detecting programming languages
     * @param skillExtractionEngine   Engine for extracting skills
     */
    public RepositoryAnalysisService(
        LanguageDetectionEngine languageDetectionEngine,
        SkillExtractionEngine skillExtractionEngine
    ) {
        this.languageDetectionEngine = languageDetectionEngine;
        this.skillExtractionEngine = skillExtractionEngine;
    }

    /**
     * Analyzes a repository snapshot and extracts languages and skills.
     * Performs two-stage analysis: language detection followed by skill extraction.
     *
     * @param snapshot The repository snapshot to analyze
     * @return AnalysisResult containing detected languages and extracted skills
     * @throws IllegalArgumentException if snapshot is null
     */
    public AnalysisResult analyzeRepository(RepositorySnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("Repository snapshot must not be null");
        }

        log.info("Starting analysis for snapshot {}", snapshot.getId());

        AnalysisResult result = new AnalysisResult(snapshot.getId());

        // Stage 1: Detect languages from snapshot metadata
        List<LanguageDetectionResult> languages = languageDetectionEngine.detectLanguages(
            snapshot.getLanguages()
        );
        result.setLanguages(languages);
        log.debug("Detected {} languages for snapshot {}", languages.size(), snapshot.getId());

        // Stage 2: Extract skills based on detected languages and package information
        String packageInfo = buildPackageInfo(snapshot);
        List<SkillScore> skills = skillExtractionEngine.extractSkills(languages, packageInfo);
        result.setSkills(skills);
        log.debug("Extracted {} skills for snapshot {}", skills.size(), snapshot.getId());

        log.info("Analysis completed for snapshot {} - {} languages, {} skills",
            snapshot.getId(), languages.size(), skills.size());

        return result;
    }

    /**
     * Builds a concatenated string of package information from snapshot metadata.
     * This string is used for skill indicator matching.
     *
     * Currently uses language information as evidence.
     * In future iterations, could parse pom.xml, package.json, requirements.txt, etc.
     *
     * @param snapshot The repository snapshot
     * @return Concatenated package information string
     */
    private String buildPackageInfo(RepositorySnapshot snapshot) {
        StringBuilder sb = new StringBuilder();

        Map<String, Integer> languages = snapshot.getLanguages();
        if (languages != null && !languages.isEmpty()) {
            languages.forEach((lang, count) -> {
                sb.append(lang).append(" ");
            });
        }

        return sb.toString();
    }
}
