package com.learnhub.assessment.engine;

import com.learnhub.github.entity.RepositorySnapshot;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CompetencyScoringEngine.
 *
 * Tests the aggregation and scoring logic:
 * - Overall level calculation
 * - Gap collection
 * - Next steps generation
 * - Confidence averaging
 */
@ExtendWith(MockitoExtension.class)
@Slf4j
class CompetencyScoringEngineTest {

    @Mock
    private DetectorRegistry detectorRegistry;

    @Mock
    private CompetencyDetector detector1;

    @Mock
    private CompetencyDetector detector2;

    @Mock
    private CompetencyDetector detector3;

    private CompetencyScoringEngine engine;
    private RepositorySnapshot testSnapshot;

    @BeforeEach
    void setUp() {
        engine = new CompetencyScoringEngine(detectorRegistry);

        testSnapshot = RepositorySnapshot.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .repositoryId(UUID.randomUUID())
                .ownerUsername("test-owner")
                .repositoryName("test-repo")
                .branchName("main")
                .commitSha("abc123")
                .filesContent(new HashMap<>())
                .createdAt(Instant.now())
                .build();

        // Setup default detector mocks
        when(detector1.getCompetencyName()).thenReturn("api-design");
        when(detector2.getCompetencyName()).thenReturn("database");
        when(detector3.getCompetencyName()).thenReturn("auth-patterns");
    }

    // ========== Overall Level Calculation Tests ==========

    @Test
    void testCalculateOverallLevel_Proficient_When_70Percent_Proficient() {
        // Setup: 2 detectors PROFICIENT, 1 EMERGING = 66% PROFICIENT+
        when(detectorRegistry.getEnabledDetectors())
                .thenReturn(List.of(detector1, detector2, detector3));

        CompetencyDetector.DetectionResult result1 = new CompetencyDetector.DetectionResult(
                "api-design", CompetencyDetector.CompetencyLevel.PROFICIENT);
        result1.confidence = 100;

        CompetencyDetector.DetectionResult result2 = new CompetencyDetector.DetectionResult(
                "database", CompetencyDetector.CompetencyLevel.PROFICIENT);
        result2.confidence = 100;

        CompetencyDetector.DetectionResult result3 = new CompetencyDetector.DetectionResult(
                "auth-patterns", CompetencyDetector.CompetencyLevel.EMERGING);
        result3.confidence = 75;

        when(detector1.analyze(testSnapshot, any())).thenReturn(result1);
        when(detector2.analyze(testSnapshot, any())).thenReturn(result2);
        when(detector3.analyze(testSnapshot, any())).thenReturn(result3);

        CompetencyScoringEngine.AssessmentResult output = engine.evaluate(testSnapshot, "1.0.0");

        assertEquals(CompetencyDetector.CompetencyLevel.PROFICIENT, output.overallLevel);
    }

    @Test
    void testCalculateOverallLevel_Emerging_When_50Percent_Proficient() {
        // Setup: 1 PROFICIENT, 2 EMERGING = 33% PROFICIENT+
        when(detectorRegistry.getEnabledDetectors())
                .thenReturn(List.of(detector1, detector2, detector3));

        CompetencyDetector.DetectionResult result1 = new CompetencyDetector.DetectionResult(
                "api-design", CompetencyDetector.CompetencyLevel.PROFICIENT);
        result1.confidence = 100;

        CompetencyDetector.DetectionResult result2 = new CompetencyDetector.DetectionResult(
                "database", CompetencyDetector.CompetencyLevel.EMERGING);
        result2.confidence = 70;

        CompetencyDetector.DetectionResult result3 = new CompetencyDetector.DetectionResult(
                "auth-patterns", CompetencyDetector.CompetencyLevel.EMERGING);
        result3.confidence = 70;

        when(detector1.analyze(testSnapshot, any())).thenReturn(result1);
        when(detector2.analyze(testSnapshot, any())).thenReturn(result2);
        when(detector3.analyze(testSnapshot, any())).thenReturn(result3);

        CompetencyScoringEngine.AssessmentResult output = engine.evaluate(testSnapshot, "1.0.0");

        assertEquals(CompetencyDetector.CompetencyLevel.EMERGING, output.overallLevel);
    }

    @Test
    void testCalculateOverallLevel_NotDemonstrated_When_Less_Than_50Percent() {
        // Setup: All NOT_DEMONSTRATED = 0% PROFICIENT+
        when(detectorRegistry.getEnabledDetectors())
                .thenReturn(List.of(detector1, detector2, detector3));

        CompetencyDetector.DetectionResult result1 = new CompetencyDetector.DetectionResult(
                "api-design", CompetencyDetector.CompetencyLevel.NOT_DEMONSTRATED);
        result1.confidence = 50;

        CompetencyDetector.DetectionResult result2 = new CompetencyDetector.DetectionResult(
                "database", CompetencyDetector.CompetencyLevel.NOT_DEMONSTRATED);
        result2.confidence = 50;

        CompetencyDetector.DetectionResult result3 = new CompetencyDetector.DetectionResult(
                "auth-patterns", CompetencyDetector.CompetencyLevel.NOT_DEMONSTRATED);
        result3.confidence = 50;

        when(detector1.analyze(testSnapshot, any())).thenReturn(result1);
        when(detector2.analyze(testSnapshot, any())).thenReturn(result2);
        when(detector3.analyze(testSnapshot, any())).thenReturn(result3);

        CompetencyScoringEngine.AssessmentResult output = engine.evaluate(testSnapshot, "1.0.0");

        assertEquals(CompetencyDetector.CompetencyLevel.NOT_DEMONSTRATED, output.overallLevel);
    }

    @Test
    void testCalculateOverallLevel_Advanced_When_All_Advanced() {
        // Setup: All ADVANCED = 100% at or above PROFICIENT
        when(detectorRegistry.getEnabledDetectors())
                .thenReturn(List.of(detector1, detector2, detector3));

        CompetencyDetector.DetectionResult result1 = new CompetencyDetector.DetectionResult(
                "api-design", CompetencyDetector.CompetencyLevel.ADVANCED);
        result1.confidence = 100;

        CompetencyDetector.DetectionResult result2 = new CompetencyDetector.DetectionResult(
                "database", CompetencyDetector.CompetencyLevel.ADVANCED);
        result2.confidence = 100;

        CompetencyDetector.DetectionResult result3 = new CompetencyDetector.DetectionResult(
                "auth-patterns", CompetencyDetector.CompetencyLevel.ADVANCED);
        result3.confidence = 100;

        when(detector1.analyze(testSnapshot, any())).thenReturn(result1);
        when(detector2.analyze(testSnapshot, any())).thenReturn(result2);
        when(detector3.analyze(testSnapshot, any())).thenReturn(result3);

        CompetencyScoringEngine.AssessmentResult output = engine.evaluate(testSnapshot, "1.0.0");

        assertEquals(CompetencyDetector.CompetencyLevel.PROFICIENT, output.overallLevel);
    }

    // ========== Gap Collection Tests ==========

    @Test
    void testCollectAllGaps_GroupsByCompetency() {
        when(detectorRegistry.getEnabledDetectors())
                .thenReturn(List.of(detector1, detector2));

        CompetencyDetector.DetectionResult result1 = new CompetencyDetector.DetectionResult(
                "api-design", CompetencyDetector.CompetencyLevel.EMERGING);
        result1.gaps = List.of("Missing error handling", "No rate limiting");
        result1.confidence = 70;

        CompetencyDetector.DetectionResult result2 = new CompetencyDetector.DetectionResult(
                "database", CompetencyDetector.CompetencyLevel.EMERGING);
        result2.gaps = List.of("Schema not normalized");
        result2.confidence = 70;

        when(detector1.analyze(testSnapshot, any())).thenReturn(result1);
        when(detector2.analyze(testSnapshot, any())).thenReturn(result2);

        CompetencyScoringEngine.AssessmentResult output = engine.evaluate(testSnapshot, "1.0.0");

        assertEquals(3, output.gaps.size());
        assertTrue(output.gaps.contains("[api-design] Missing error handling"));
        assertTrue(output.gaps.contains("[api-design] No rate limiting"));
        assertTrue(output.gaps.contains("[database] Schema not normalized"));
    }

    @Test
    void testCollectAllGaps_Empty_When_No_Gaps() {
        when(detectorRegistry.getEnabledDetectors())
                .thenReturn(List.of(detector1));

        CompetencyDetector.DetectionResult result1 = new CompetencyDetector.DetectionResult(
                "api-design", CompetencyDetector.CompetencyLevel.PROFICIENT);
        result1.gaps = new ArrayList<>();  // No gaps
        result1.confidence = 100;

        when(detector1.analyze(testSnapshot, any())).thenReturn(result1);

        CompetencyScoringEngine.AssessmentResult output = engine.evaluate(testSnapshot, "1.0.0");

        assertEquals(0, output.gaps.size());
    }

    // ========== Next Steps Generation Tests ==========

    @Test
    void testGenerateNextSteps_PrioritizesNotDemonstrated() {
        when(detectorRegistry.getEnabledDetectors())
                .thenReturn(List.of(detector1, detector2, detector3));

        CompetencyDetector.DetectionResult result1 = new CompetencyDetector.DetectionResult(
                "api-design", CompetencyDetector.CompetencyLevel.NOT_DEMONSTRATED);
        result1.gaps = List.of("No REST patterns found");
        result1.confidence = 80;

        CompetencyDetector.DetectionResult result2 = new CompetencyDetector.DetectionResult(
                "database", CompetencyDetector.CompetencyLevel.PROFICIENT);
        result2.gaps = new ArrayList<>();
        result2.confidence = 100;

        CompetencyDetector.DetectionResult result3 = new CompetencyDetector.DetectionResult(
                "auth-patterns", CompetencyDetector.CompetencyLevel.EMERGING);
        result3.gaps = List.of("Missing JWT validation");
        result3.confidence = 70;

        when(detector1.analyze(testSnapshot, any())).thenReturn(result1);
        when(detector2.analyze(testSnapshot, any())).thenReturn(result2);
        when(detector3.analyze(testSnapshot, any())).thenReturn(result3);

        CompetencyScoringEngine.AssessmentResult output = engine.evaluate(testSnapshot, "1.0.0");

        assertTrue(output.nextSteps.size() > 0);
        // First step should mention api-design (NOT_DEMONSTRATED)
        assertTrue(output.nextSteps.get(0).contains("api-design"));
    }

    @Test
    void testGenerateNextSteps_SuccessMessage_When_All_Proficient() {
        when(detectorRegistry.getEnabledDetectors())
                .thenReturn(List.of(detector1, detector2));

        CompetencyDetector.DetectionResult result1 = new CompetencyDetector.DetectionResult(
                "api-design", CompetencyDetector.CompetencyLevel.PROFICIENT);
        result1.gaps = new ArrayList<>();
        result1.confidence = 100;

        CompetencyDetector.DetectionResult result2 = new CompetencyDetector.DetectionResult(
                "database", CompetencyDetector.CompetencyLevel.PROFICIENT);
        result2.gaps = new ArrayList<>();
        result2.confidence = 100;

        when(detector1.analyze(testSnapshot, any())).thenReturn(result1);
        when(detector2.analyze(testSnapshot, any())).thenReturn(result2);

        CompetencyScoringEngine.AssessmentResult output = engine.evaluate(testSnapshot, "1.0.0");

        assertTrue(output.nextSteps.size() > 0);
        assertTrue(output.nextSteps.get(0).contains("Congratulations"));
    }

    // ========== Confidence Calculation Tests ==========

    @Test
    void testCalculateAverageConfidence() {
        when(detectorRegistry.getEnabledDetectors())
                .thenReturn(List.of(detector1, detector2, detector3));

        CompetencyDetector.DetectionResult result1 = new CompetencyDetector.DetectionResult(
                "api-design", CompetencyDetector.CompetencyLevel.PROFICIENT);
        result1.confidence = 100;

        CompetencyDetector.DetectionResult result2 = new CompetencyDetector.DetectionResult(
                "database", CompetencyDetector.CompetencyLevel.PROFICIENT);
        result2.confidence = 80;

        CompetencyDetector.DetectionResult result3 = new CompetencyDetector.DetectionResult(
                "auth-patterns", CompetencyDetector.CompetencyLevel.PROFICIENT);
        result3.confidence = 90;

        when(detector1.analyze(testSnapshot, any())).thenReturn(result1);
        when(detector2.analyze(testSnapshot, any())).thenReturn(result2);
        when(detector3.analyze(testSnapshot, any())).thenReturn(result3);

        CompetencyScoringEngine.AssessmentResult output = engine.evaluate(testSnapshot, "1.0.0");

        // Average of 100, 80, 90 = 90
        assertEquals(90.0, output.overallConfidence, 0.01);
    }

    // ========== Error Handling Tests ==========

    @Test
    void testDetectorFailure_ContinuesWithOtherDetectors() {
        when(detectorRegistry.getEnabledDetectors())
                .thenReturn(List.of(detector1, detector2, detector3));

        CompetencyDetector.DetectionResult result1 = new CompetencyDetector.DetectionResult(
                "api-design", CompetencyDetector.CompetencyLevel.PROFICIENT);
        result1.confidence = 100;

        // detector2 throws exception
        when(detector2.analyze(testSnapshot, any()))
                .thenThrow(new RuntimeException("Detector error"));

        CompetencyDetector.DetectionResult result3 = new CompetencyDetector.DetectionResult(
                "auth-patterns", CompetencyDetector.CompetencyLevel.PROFICIENT);
        result3.confidence = 100;

        when(detector1.analyze(testSnapshot, any())).thenReturn(result1);
        when(detector3.analyze(testSnapshot, any())).thenReturn(result3);

        CompetencyScoringEngine.AssessmentResult output = engine.evaluate(testSnapshot, "1.0.0");

        // Should have 3 results (1 error case = NOT_DEMONSTRATED)
        assertEquals(3, output.detections.size());
        // Database should show NOT_DEMONSTRATED with error message in gaps
        assertEquals(
                CompetencyDetector.CompetencyLevel.NOT_DEMONSTRATED,
                output.detections.get("database").level
        );
    }

    @Test
    void testEvaluate_EmptyDetectorList_ReturnsNotDemonstrated() {
        when(detectorRegistry.getEnabledDetectors())
                .thenReturn(new ArrayList<>());

        CompetencyScoringEngine.AssessmentResult output = engine.evaluate(testSnapshot, "1.0.0");

        assertEquals(CompetencyDetector.CompetencyLevel.NOT_DEMONSTRATED, output.overallLevel);
        assertEquals(testSnapshot.getId(), output.repoId);
        assertEquals("1.0.0", output.brfVersion);
    }

    @Test
    void testEvaluate_IncludesTimestampAndRepoId() {
        when(detectorRegistry.getEnabledDetectors())
                .thenReturn(List.of(detector1));

        CompetencyDetector.DetectionResult result1 = new CompetencyDetector.DetectionResult(
                "api-design", CompetencyDetector.CompetencyLevel.PROFICIENT);
        result1.confidence = 100;

        when(detector1.analyze(testSnapshot, any())).thenReturn(result1);

        CompetencyScoringEngine.AssessmentResult output = engine.evaluate(testSnapshot, "1.0.0");

        assertNotNull(output.timestamp);
        assertEquals(testSnapshot.getId(), output.repoId);
        assertEquals("1.0.0", output.brfVersion);
    }

    @Test
    void testEvaluate_AllDetectorsIncludedInDetections() {
        when(detectorRegistry.getEnabledDetectors())
                .thenReturn(List.of(detector1, detector2, detector3));

        CompetencyDetector.DetectionResult result1 = new CompetencyDetector.DetectionResult(
                "api-design", CompetencyDetector.CompetencyLevel.PROFICIENT);
        result1.confidence = 100;

        CompetencyDetector.DetectionResult result2 = new CompetencyDetector.DetectionResult(
                "database", CompetencyDetector.CompetencyLevel.EMERGING);
        result2.confidence = 70;

        CompetencyDetector.DetectionResult result3 = new CompetencyDetector.DetectionResult(
                "auth-patterns", CompetencyDetector.CompetencyLevel.NOT_DEMONSTRATED);
        result3.confidence = 50;

        when(detector1.analyze(testSnapshot, any())).thenReturn(result1);
        when(detector2.analyze(testSnapshot, any())).thenReturn(result2);
        when(detector3.analyze(testSnapshot, any())).thenReturn(result3);

        CompetencyScoringEngine.AssessmentResult output = engine.evaluate(testSnapshot, "1.0.0");

        assertEquals(3, output.detections.size());
        assertTrue(output.detections.containsKey("api-design"));
        assertTrue(output.detections.containsKey("database"));
        assertTrue(output.detections.containsKey("auth-patterns"));
    }
}
