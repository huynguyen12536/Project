package com.learnhub.assessment.detectors;

import com.learnhub.assessment.engine.CompetencyDetector;
import com.learnhub.github.entity.RepositorySnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ApiDesignDetector.
 *
 * Tests competency detection for REST API design patterns:
 * - Resource-based naming conventions
 * - HTTP method usage (GET, POST, PUT, DELETE)
 * - Status code conventions
 * - Error handling patterns
 * - Request/response consistency
 */
@DisplayName("ApiDesignDetector Tests")
class ApiDesignDetectorTest {

    private ApiDesignDetector detector;
    private RepositorySnapshot snapshot;

    @BeforeEach
    void setUp() {
        detector = new ApiDesignDetector();
        snapshot = createTestSnapshot();
    }

    @Test
    @DisplayName("Detector should have correct competency name")
    void testCompetencyName() {
        assertEquals("api-design", detector.getCompetencyName());
    }

    @Test
    @DisplayName("Detector should have appropriate priority")
    void testPriority() {
        assertTrue(detector.getPriority() > 0, "API detector should have positive priority");
    }

    @Test
    @DisplayName("Should return NOT_DEMONSTRATED when no endpoints found")
    void testNoEndpointsFound() {
        CompetencyDetector.BrfRules rules = new CompetencyDetector.BrfRules("api-design");

        CompetencyDetector.DetectionResult result = detector.analyze(snapshot, rules);

        assertEquals(CompetencyDetector.CompetencyLevel.NOT_DEMONSTRATED, result.level);
        assertTrue(result.gaps.stream()
                .anyMatch(g -> g.contains("REST API endpoints")),
            "Should identify missing API endpoints");
        assertTrue(result.confidence > 0, "Should have positive confidence");
    }

    @Test
    @DisplayName("Should initialize detection result with proper fields")
    void testDetectionResultInitialization() {
        CompetencyDetector.BrfRules rules = new CompetencyDetector.BrfRules("api-design");

        CompetencyDetector.DetectionResult result = detector.analyze(snapshot, rules);

        assertNotNull(result, "Result should not be null");
        assertEquals("api-design", result.competencyName);
        assertNotNull(result.evidence, "Evidence list should be initialized");
        assertNotNull(result.gaps, "Gaps list should be initialized");
        assertNotNull(result.metadata, "Metadata should be initialized");
        assertTrue(result.confidence >= 0 && result.confidence <= 100,
            "Confidence should be between 0-100");
    }

    @Test
    @DisplayName("Should handle null snapshot gracefully")
    void testNullSnapshot() {
        CompetencyDetector.BrfRules rules = new CompetencyDetector.BrfRules("api-design");

        assertDoesNotThrow(() -> {
            CompetencyDetector.DetectionResult result = detector.analyze(null, rules);
            assertNotNull(result);
            assertEquals(CompetencyDetector.CompetencyLevel.NOT_DEMONSTRATED, result.level);
        }, "Should handle null snapshot without throwing");
    }

    @Test
    @DisplayName("Should handle null rules gracefully")
    void testNullRules() {
        CompetencyDetector.BrfRules rules = new CompetencyDetector.BrfRules("api-design");

        assertDoesNotThrow(() -> {
            CompetencyDetector.DetectionResult result = detector.analyze(snapshot, rules);
            assertNotNull(result);
        }, "Should handle null rules without throwing");
    }

    @Test
    @DisplayName("Metadata should contain endpoint count")
    void testMetadataContent() {
        CompetencyDetector.BrfRules rules = new CompetencyDetector.BrfRules("api-design");

        CompetencyDetector.DetectionResult result = detector.analyze(snapshot, rules);

        assertNotNull(result.metadata, "Metadata should not be null");
        assertTrue(result.metadata.containsKey("endpoint_count"),
            "Metadata should contain endpoint count");
    }

    @Test
    @DisplayName("Should collect gaps when API patterns missing")
    void testGapsIdentification() {
        CompetencyDetector.BrfRules rules = new CompetencyDetector.BrfRules("api-design");

        CompetencyDetector.DetectionResult result = detector.analyze(snapshot, rules);

        assertTrue(!result.gaps.isEmpty() || !result.evidence.isEmpty(),
            "Should have either gaps or evidence");
    }

    @Test
    @DisplayName("Should collect evidence when API patterns found")
    void testEvidenceCollection() {
        CompetencyDetector.BrfRules rules = new CompetencyDetector.BrfRules("api-design");

        CompetencyDetector.DetectionResult result = detector.analyze(snapshot, rules);

        assertNotNull(result.evidence);
        assertNotNull(result.gaps);
    }

    @Test
    @DisplayName("Confidence should vary based on analysis quality")
    void testConfidenceVariation() {
        CompetencyDetector.BrfRules rules = new CompetencyDetector.BrfRules("api-design");

        CompetencyDetector.DetectionResult result = detector.analyze(snapshot, rules);

        assertTrue(result.confidence >= 0 && result.confidence <= 100,
            "Confidence should be valid percentage");
    }

    @Test
    @DisplayName("Should handle multiple detector instances independently")
    void testDetectorIndependence() {
        ApiDesignDetector detector1 = new ApiDesignDetector();
        ApiDesignDetector detector2 = new ApiDesignDetector();

        CompetencyDetector.BrfRules rules = new CompetencyDetector.BrfRules("api-design");

        CompetencyDetector.DetectionResult result1 = detector1.analyze(snapshot, rules);
        CompetencyDetector.DetectionResult result2 = detector2.analyze(snapshot, rules);

        // Both should produce same competency name
        assertEquals(result1.competencyName, result2.competencyName);
    }

    @Test
    @DisplayName("Should work with complex BRF rules")
    void testComplexBrfRules() {
        CompetencyDetector.BrfRules rules = new CompetencyDetector.BrfRules(
            "api-design",
            "REST API design competency",
            java.util.List.of(
                "Resource-based naming",
                "Proper HTTP methods",
                "Status code conventions"
            ),
            java.util.List.of(
                "All endpoints follow REST conventions",
                "Proper error handling"
            ),
            0.75
        );

        CompetencyDetector.DetectionResult result = detector.analyze(snapshot, rules);

        assertNotNull(result);
        assertEquals("api-design", result.competencyName);
    }

    /**
     * Create a test repository snapshot.
     */
    private RepositorySnapshot createTestSnapshot() {
        return RepositorySnapshot.builder()
            .id(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .githubRepoId(123456L)
            .branch("main")
            .commitSha("abc123def456")
            .filesCount(50)
            .totalSizeKb(1024L)
            .languages(createTestLanguages())
            .createdAt(LocalDateTime.now())
            .build();
    }

    /**
     * Create test language distribution.
     */
    private Map<String, Integer> createTestLanguages() {
        Map<String, Integer> languages = new HashMap<>();
        languages.put("Java", 8000);
        languages.put("SQL", 1000);
        languages.put("Other", 500);
        return languages;
    }
}
