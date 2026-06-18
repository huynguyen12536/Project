package com.learnhub.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object representing the result of language detection analysis.
 * Contains information about a detected programming language and its prevalence in the repository.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LanguageDetectionResult {

    /**
     * Name of the detected programming language (e.g., "Java", "JavaScript", "Python")
     */
    private String language;

    /**
     * Number of files using this language in the analyzed repository
     */
    private Integer fileCount;

    /**
     * Percentage of files using this language (0-100, rounded to 2 decimal places)
     */
    private Double percentage;

    /**
     * Evidence of language detection (e.g., "X files detected")
     */
    private List<String> evidence;
}
