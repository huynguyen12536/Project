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

}
