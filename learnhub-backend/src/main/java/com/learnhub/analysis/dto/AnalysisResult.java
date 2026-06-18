package com.learnhub.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object representing the complete analysis result for a repository snapshot.
 * Contains detected languages and extracted skills with confidence scores.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResult {

    /**
     * Unique identifier of the repository snapshot being analyzed
     */
    private UUID snapshotId;

    /**
     * List of detected programming languages with file counts and percentages
     */
    private List<LanguageDetectionResult> languages;

    /**
     * List of detected skills with confidence scores
     */
    private List<SkillScore> skills;

    /**
     * Timestamp when the analysis was performed
     */
    private LocalDateTime analyzedAt;

    /**
     * Constructor initializing empty collections and current timestamp.
     *
     * @param snapshotId Unique identifier of the snapshot
     */
    public AnalysisResult(UUID snapshotId) {
        this.snapshotId = snapshotId;
        this.languages = new ArrayList<>();
        this.skills = new ArrayList<>();
        this.analyzedAt = LocalDateTime.now();
    }
}
