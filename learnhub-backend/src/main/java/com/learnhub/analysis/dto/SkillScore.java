package com.learnhub.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing a detected skill with a confidence score.
 * Uses deterministic indicator-based scoring with no AI/ML components.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillScore {

    /**
     * Name of the detected skill (e.g., "Spring Boot", "React", "Django")
     */
    private String skillName;

    /**
     * Confidence score for the skill detection (0-100).
     * Calculated as: (matched_indicators / total_indicators) * 100
     */
    private Integer confidence;

    /**
     * Evidence supporting the skill detection (e.g., "2/3 indicators found")
     */
    private String evidence;

    /**
     * Primary language associated with the skill (e.g., "Java", "JavaScript", "Python")
     */
    private String language;

    /**
     * Constructor with validation for confidence range.
     *
     * @param skillName  Name of the skill
     * @param confidence Confidence score (must be 0-100)
     * @param evidence   Evidence string
     * @param language   Primary language
     * @throws IllegalArgumentException if confidence is outside 0-100 range
     */
    public SkillScore(String skillName, Integer confidence, String evidence, String language) {
        if (confidence < 0 || confidence > 100) {
            throw new IllegalArgumentException("Confidence must be between 0 and 100");
        }
        this.skillName = skillName;
        this.confidence = confidence;
        this.evidence = evidence;
        this.language = language;
    }
}
