package com.learnhub.assessment.engine;

import com.learnhub.github.entity.RepositorySnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pluggable interface for competency detectors.
 *
 * Each detector analyzes a specific competency (e.g., API Design, Database)
 * against BRF rules and returns evidence of demonstrated skills.
 *
 * Implementation Pattern:
 * 1. Create class implementing CompetencyDetector
 * 2. Add @Component annotation for Spring auto-discovery
 * 3. Implement getCompetencyName() (unique identifier)
 * 4. Implement analyze() (evaluation logic)
 * 5. Detectors are auto-discovered and wired into AssessmentEngine
 *
 * Example:
 * @Component
 * public class ApiDesignDetector implements CompetencyDetector {
 *     @Override public String getCompetencyName() { return "api-design"; }
 *     @Override public DetectionResult analyze(...) { ... }
 * }
 */
public interface CompetencyDetector {

    /**
     * Unique identifier for this detector's competency.
     *
     * Examples:
     * - "api-design" — REST API conventions and patterns
     * - "database" — SQL schema design, normalization
     * - "auth-patterns" — Authentication and security
     * - "code-organization" — Package structure, naming
     * - "business-logic" — Feature completeness, edge cases
     * - "documentation" — Code comments, docs, README
     * - "test-coverage" — Unit tests, test quality
     *
     * @return Unique competency identifier (kebab-case)
     */
    String getCompetencyName();

    /**
     * Analyze repository for this competency.
     *
     * Core method: examines repository source code and structure
     * against BRF rules for this competency. Returns detailed
     * detection result with evidence, gaps, and confidence.
     *
     * Called by AssessmentEngine during evaluation.
     * May be called in parallel with other detectors.
     *
     * @param snapshot Repository snapshot (source code + metadata)
     * @param rules BRF rules for this competency (defining expected patterns)
     * @return DetectionResult with evidence, gaps, and confidence
     */
    DetectionResult analyze(RepositorySnapshot snapshot, BrfRules rules);

    /**
     * Detector execution priority (for ordering and dependencies).
     *
     * Higher priority runs first. Useful if some detectors depend
     * on results from other detectors (e.g., API detector depends on code being parsed).
     *
     * Default: 0 (no priority). Can override for custom ordering.
     *
     * @return Priority score (higher = earlier execution)
     */
    default int getPriority() {
        return 0;
    }

    /**
     * Result of single competency analysis.
     *
     * Contains evidence found, gaps identified, competency level,
     * and confidence score for this specific detector.
     */
    /**
     * Result of single competency analysis.
     *
     * Contains evidence found, gaps identified, competency level,
     * and confidence score for this specific detector.
     */
    class DetectionResult {
        /**
         * Competency name (maps to detector.getCompetencyName())
         */
        public String competencyName;

        /**
         * Competency level based on evidence found.
         *
         * NOT_DEMONSTRATED: No evidence found for this competency
         * EMERGING: Partial/inconsistent implementation
         * PROFICIENT: Solid, consistent implementation
         * ADVANCED: Advanced patterns, exceeds expectations
         */
        public CompetencyLevel level;

        /**
         * Specific evidence of demonstrated competency.
         *
         * Examples:
         * - "Endpoints follow REST naming conventions (/users, /posts)"
         * - "Database uses foreign keys for relationships"
         * - "Authentication enforced with JWT tokens"
         *
         * Used to explain to learner what was found.
         */
        public List<String> evidence;

        /**
         * Identified gaps / missing implementations.
         *
         * Examples:
         * - "Missing error handling for edge cases"
         * - "Database schema not normalized (3NF)"
         * - "No rate limiting on sensitive endpoints"
         *
         * Used to guide learner on next steps.
         */
        public List<String> gaps;

        /**
         * Confidence in this assessment (0-100).
         *
         * 100: High confidence (code patterns clear, unambiguous)
         * 70: Moderate confidence (some interpretation needed)
         * 40: Low confidence (analysis incomplete or ambiguous)
         *
         * Used by ScoringEngine to weight detector results.
         */
        public int confidence;

        /**
         * Detector-specific metadata.
         *
         * Optional: Additional data for debugging or future analysis.
         * Examples:
         * - "endpoint_count": 12
         * - "test_coverage": 75.5
         * - "normalized_form": "3NF"
         */
        public Map<String, Object> metadata;

        /**
         * Default constructor.
         */
        public DetectionResult() {
            this.evidence = new ArrayList<>();
            this.gaps = new ArrayList<>();
            this.metadata = new HashMap<>();
            this.confidence = 0;
        }

        /**
         * Constructor with competency name and level.
         *
         * @param competencyName the name of this competency
         * @param level the detected competency level
         */
        public DetectionResult(String competencyName, CompetencyLevel level) {
            this();
            this.competencyName = competencyName;
            this.level = level;
        }
    }

    /**
     * Competency level enumeration.
     *
     * Maps evidence found to skill level.
     */
    enum CompetencyLevel {
        /**
         * No evidence found for this competency.
         * Learner should begin learning this skill.
         */
        NOT_DEMONSTRATED(0),

        /**
         * Partial or inconsistent implementation.
         * Learner has started but needs improvement.
         */
        EMERGING(1),

        /**
         * Solid, consistent implementation.
         * Learner demonstrates this competency well.
         */
        PROFICIENT(2),

        /**
         * Advanced patterns, exceeds expectations.
         * Learner shows exceptional skill in this area.
         */
        ADVANCED(3);

        public final int score;

        CompetencyLevel(int score) {
            this.score = score;
        }
    }

    /**
     * BRF Rules for a competency.
     *
     * Defines expected patterns and acceptance criteria for this competency.
     * Loaded from BRF YAML; passed to detector for analysis.
     *
     * Example fields (from YAML):
     * - requiredPatterns: ["REST naming", "proper HTTP methods"]
     * - acceptanceCriteria: ["All endpoints return 200/400/500", ...]
     * - minConfidence: 0.7
     * - examples: [code snippets showing good implementation]
     */
    class BrfRules {
        /**
         * Unique competency identifier (e.g., "api-design", "database").
         */
        public String competencyName;

        /**
         * Human-readable description of this competency.
         */
        public String description;

        /**
         * Required patterns to demonstrate this competency.
         * Each detector interprets these based on their specialty.
         */
        public List<String> requiredPatterns;

        /**
         * Acceptance criteria for marking this competency as demonstrated.
         * Detector must verify all or most of these are met.
         */
        public List<String> acceptanceCriteria;

        /**
         * Minimum confidence threshold (0.0 - 1.0).
         * Detector results below this are considered low-confidence.
         */
        public double minConfidence;

        /**
         * Code examples showing good/bad patterns for this competency.
         * Maps example name to code snippet or description.
         */
        public Map<String, String> examples;

        /**
         * Default constructor.
         */
        public BrfRules() {
            this.requiredPatterns = new ArrayList<>();
            this.acceptanceCriteria = new ArrayList<>();
            this.examples = new HashMap<>();
            this.minConfidence = 0.7;
        }

        /**
         * Constructor with competency name.
         *
         * @param competencyName the competency identifier
         */
        public BrfRules(String competencyName) {
            this();
            this.competencyName = competencyName;
        }

        /**
         * Constructor with full details.
         *
         * @param competencyName the competency identifier
         * @param description human-readable description
         * @param requiredPatterns list of required patterns
         * @param acceptanceCriteria list of acceptance criteria
         * @param minConfidence minimum confidence threshold
         */
        public BrfRules(String competencyName, String description,
                        List<String> requiredPatterns, List<String> acceptanceCriteria,
                        double minConfidence) {
            this.competencyName = competencyName;
            this.description = description;
            this.requiredPatterns = requiredPatterns != null ? requiredPatterns : new ArrayList<>();
            this.acceptanceCriteria = acceptanceCriteria != null ? acceptanceCriteria : new ArrayList<>();
            this.minConfidence = minConfidence;
            this.examples = new HashMap<>();
        }
    }
}
