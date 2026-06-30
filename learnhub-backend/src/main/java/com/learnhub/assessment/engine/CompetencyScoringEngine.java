package com.learnhub.assessment.engine;

import com.learnhub.github.entity.RepositorySnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Assessment Scoring Engine.
 *
 * Orchestrates the complete evaluation process:
 * 1. Runs all competency detectors in parallel/sequence
 * 2. Aggregates detector results
 * 3. Calculates overall competency level
 * 4. Identifies gaps and next steps
 * 5. Generates human-readable feedback
 *
 * Uses DetectorRegistry to auto-discover and manage detectors.
 * Supports selective detector execution via registry enable/disable.
 *
 * Used by AssessmentJobConsumer to evaluate repositories.
 */
@Service
@Slf4j
public class CompetencyScoringEngine {

    private final DetectorRegistry detectorRegistry;

    /**
     * Create assessment engine with detector registry.
     *
     * @param detectorRegistry manages all available detectors
     */
    public CompetencyScoringEngine(DetectorRegistry detectorRegistry) {
        this.detectorRegistry = detectorRegistry;
    }

    /**
     * Evaluate a repository snapshot against BRF rules.
     *
     * @param snapshot Repository snapshot (source code + metadata)
     * @param brfVersion BRF version to evaluate against (e.g., "1.0.0")
     * @return AssessmentResult with competency levels, evidence, gaps, next steps
     */
    public AssessmentResult evaluate(RepositorySnapshot snapshot, String brfVersion) {
        AssessmentResult result = new AssessmentResult();
        result.timestamp = Instant.now();
        result.repoId = snapshot.getId();
        result.brfVersion = brfVersion;

        Map<String, CompetencyDetector.DetectionResult> detections = new HashMap<>();

        log.info("Starting assessment evaluation: repo={}, brf={}", snapshot.getId(), brfVersion);
        long startTime = System.currentTimeMillis();

        try {
            // TODO: Load BRF rules by version
            // BrfVersion brf = brfService.getVersion(brfVersion);
            // List<CompetencyDetector.BrfRules> rulesPerCompetency = brf.getRules();

            // Get enabled detectors from registry (already ordered by priority)
            List<CompetencyDetector> orderedDetectors = detectorRegistry.getEnabledDetectors();

            log.debug("Running {} enabled detectors in order: {}",
                    orderedDetectors.size(),
                    orderedDetectors.stream()
                            .map(CompetencyDetector::getCompetencyName)
                            .collect(Collectors.joining(", ")));

            for (CompetencyDetector detector : orderedDetectors) {
                try {
                    String competency = detector.getCompetencyName();
                    log.debug("Running detector: {}", competency);

                    // TODO: Get rules for this competency from loaded BRF
                    // CompetencyDetector.BrfRules rules = findRulesFor(competency, brf);

                    // For now, create dummy rules
                    CompetencyDetector.BrfRules rules = new CompetencyDetector.BrfRules();
                    rules.competencyName = competency;

                    CompetencyDetector.DetectionResult detection = detector.analyze(snapshot, rules);
                    detections.put(competency, detection);

                    log.debug("Detector {} complete: level={}, confidence={}%",
                            competency, detection.level, detection.confidence);

                } catch (Exception e) {
                    log.error("Detector failed: {}", detector.getCompetencyName(), e);
                    // Continue with other detectors; create error result for this competency
                    CompetencyDetector.DetectionResult errorResult =
                            new CompetencyDetector.DetectionResult(
                                    detector.getCompetencyName(),
                                    CompetencyDetector.CompetencyLevel.NOT_DEMONSTRATED);
                    errorResult.gaps.add("Error during analysis: " + e.getMessage());
                    errorResult.confidence = 0;
                    detections.put(detector.getCompetencyName(), errorResult);
                }
            }

            // Aggregate results
            result.detections = detections;
            result.overallLevel = calculateOverallLevel(detections);
            result.gaps = collectAllGaps(detections);
            result.nextSteps = generateNextSteps(detections);
            result.overallConfidence = calculateAverageConfidence(detections);

            long elapsedMs = System.currentTimeMillis() - startTime;
            log.info("Assessment evaluation complete in {}ms: overall={}",
                    elapsedMs, result.overallLevel);

        } catch (Exception e) {
            log.error("Critical error during assessment evaluation", e);
            result.overallLevel = CompetencyDetector.CompetencyLevel.NOT_DEMONSTRATED;
            result.gaps.add("Critical error: " + e.getMessage());
            result.nextSteps.add("Contact support: assessment could not be completed");
        }

        return result;
    }

    /**
     * Calculate overall competency level from individual detector results.
     *
     * Logic:
     * - PROFICIENT: ≥70% of competencies at PROFICIENT or ADVANCED
     * - EMERGING: ≥50% at PROFICIENT+, some gaps
     * - NOT_DEMONSTRATED: <50% at PROFICIENT or most gaps
     */
    private CompetencyDetector.CompetencyLevel calculateOverallLevel(
            Map<String, CompetencyDetector.DetectionResult> detections) {

        if (detections.isEmpty()) {
            return CompetencyDetector.CompetencyLevel.NOT_DEMONSTRATED;
        }

        int advanced = 0, proficient = 0, emerging = 0, notDemonstrated = 0;

        for (CompetencyDetector.DetectionResult d : detections.values()) {
            switch (d.level) {
                case ADVANCED -> advanced++;
                case PROFICIENT -> proficient++;
                case EMERGING -> emerging++;
                case NOT_DEMONSTRATED -> notDemonstrated++;
            }
        }

        int total = detections.size();
        int atOrAboveProficient = advanced + proficient;
        double proficientPercentage = (double) atOrAboveProficient / total;

        if (proficientPercentage >= 0.7) {
            return CompetencyDetector.CompetencyLevel.PROFICIENT;
        } else if (proficientPercentage >= 0.5 || emerging > 0) {
            return CompetencyDetector.CompetencyLevel.EMERGING;
        } else {
            return CompetencyDetector.CompetencyLevel.NOT_DEMONSTRATED;
        }
    }

    /**
     * Collect all gaps from all competencies.
     */
    private List<String> collectAllGaps(Map<String, CompetencyDetector.DetectionResult> detections) {
        List<String> allGaps = new ArrayList<>();

        for (CompetencyDetector.DetectionResult d : detections.values()) {
            if (!d.gaps.isEmpty()) {
                for (String gap : d.gaps) {
                    allGaps.add(String.format("[%s] %s", d.competencyName, gap));
                }
            }
        }

        return allGaps;
    }

    /**
     * Generate next steps / recommendations for learner.
     *
     * Prioritize by competency level:
     * 1. NOT_DEMONSTRATED competencies (highest priority)
     * 2. EMERGING competencies
     * 3. Suggest supplementary resources
     */
    private List<String> generateNextSteps(Map<String, CompetencyDetector.DetectionResult> detections) {
        List<String> nextSteps = new ArrayList<>();

        // Priority 1: Focus on NOT_DEMONSTRATED
        for (CompetencyDetector.DetectionResult d : detections.values()) {
            if (d.level == CompetencyDetector.CompetencyLevel.NOT_DEMONSTRATED) {
                if (!d.gaps.isEmpty()) {
                    nextSteps.add(String.format("🔴 Learn %s: %s", d.competencyName, d.gaps.get(0)));
                } else {
                    nextSteps.add(String.format("🔴 Explore %s in your next project", d.competencyName));
                }
            }
        }

        // Priority 2: Improve EMERGING
        for (CompetencyDetector.DetectionResult d : detections.values()) {
            if (d.level == CompetencyDetector.CompetencyLevel.EMERGING) {
                if (!d.gaps.isEmpty()) {
                    nextSteps.add(String.format("🟡 Improve %s: %s", d.competencyName, d.gaps.get(0)));
                }
            }
        }

        // Priority 3: Suggestions for PROFICIENT (can skip)
        // Usually learners move to next project rather than perfecting current one

        if (nextSteps.isEmpty()) {
            nextSteps.add("✅ Congratulations! All competencies demonstrated. Move to next project for advanced skills.");
        }

        return nextSteps;
    }

    /**
     * Calculate average confidence across all detectors.
     */
    private double calculateAverageConfidence(Map<String, CompetencyDetector.DetectionResult> detections) {
        if (detections.isEmpty()) return 0;

        double totalConfidence = detections.values().stream()
                .mapToInt(d -> d.confidence)
                .sum();

        return totalConfidence / detections.size();
    }

    /**
     * Assessment evaluation result.
     *
     * Contains:
     * - Individual detector results (evidence, gaps, levels)
     * - Overall competency level
     * - Collected gaps across all competencies
     * - Next step recommendations
     * - Confidence score
     */
    public static class AssessmentResult {
        public UUID repoId;
        public String brfVersion;
        public Instant timestamp;

        // Detector results (keyed by competency name)
        public Map<String, CompetencyDetector.DetectionResult> detections = new HashMap<>();

        // Aggregated results
        public CompetencyDetector.CompetencyLevel overallLevel;
        public List<String> gaps = new ArrayList<>();
        public List<String> nextSteps = new ArrayList<>();
        public double overallConfidence;

        // Optional: related assessment metadata
        public String recommendedNextProject;
        public String learnerFeedback;
    }
}
