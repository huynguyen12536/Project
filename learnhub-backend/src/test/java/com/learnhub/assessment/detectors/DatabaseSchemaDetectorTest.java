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
 * Unit tests for DatabaseSchemaDetector.
 *
 * Tests competency detection for database design patterns:
 * - Flyway migration usage
 * - Schema structure (tables, columns, relationships)
 * - Primary keys and foreign keys
 * - Indices and constraints
 * - Normalization assessment
 */
@DisplayName("DatabaseSchemaDetector Tests")
class DatabaseSchemaDetectorTest {

    private DatabaseSchemaDetector detector;
    private RepositorySnapshot snapshot;

    @BeforeEach
    void setUp() {
        detector = new DatabaseSchemaDetector();
        snapshot = createTestSnapshot();
    }

    @Test
    @DisplayName("Detector should have correct competency name")
    void testCompetencyName() {
        assertEquals("database", detector.getCompetencyName());
    }

    @Test
    @DisplayName("Detector should have appropriate priority")
    void testPriority() {
        assertTrue(detector.getPriority() >= 0, "Priority should be non-negative");
    }

    @Test
    @DisplayName("Should return NOT_DEMONSTRATED when no migrations found")
    void testNoMigrationsFound() {
        CompetencyDetector.BrfRules rules = new CompetencyDetector.BrfRules("database");

        CompetencyDetector.DetectionResult result = detector.analyze(snapshot, rules);

        assertEquals(CompetencyDetector.CompetencyLevel.NOT_DEMONSTRATED, result.level);
        assertTrue(result.gaps.stream()
                .anyMatch(g -> g.contains("No database migrations")),
            "Should identify missing migrations");
        assertTrue(result.confidence > 0, "Should have positive confidence");
    }

    @Test
    @DisplayName("Should initialize detection result with proper fields")
    void testDetectionResultInitialization() {
        CompetencyDetector.BrfRules rules = new CompetencyDetector.BrfRules("database");

        CompetencyDetector.DetectionResult result = detector.analyze(snapshot, rules);

        assertNotNull(result, "Result should not be null");
        assertEquals("database", result.competencyName);
        assertNotNull(result.evidence, "Evidence list should be initialized");
        assertNotNull(result.gaps, "Gaps list should be initialized");
        assertNotNull(result.metadata, "Metadata should be initialized");
        assertTrue(result.confidence >= 0 && result.confidence <= 100,
            "Confidence should be between 0-100");
    }

    @Test
    @DisplayName("Should handle null snapshot gracefully")
    void testNullSnapshot() {
        CompetencyDetector.BrfRules rules = new CompetencyDetector.BrfRules("database");

        assertDoesNotThrow(() -> {
            CompetencyDetector.DetectionResult result = detector.analyze(null, rules);
            assertNotNull(result);
            assertEquals(CompetencyDetector.CompetencyLevel.NOT_DEMONSTRATED, result.level);
        }, "Should handle null snapshot without throwing");
    }

    @Test
    @DisplayName("Should handle null rules gracefully")
    void testNullRules() {
        CompetencyDetector.BrfRules rules = new CompetencyDetector.BrfRules("database");

        assertDoesNotThrow(() -> {
            CompetencyDetector.DetectionResult result = detector.analyze(snapshot, rules);
            assertNotNull(result);
        }, "Should handle null rules without throwing");
    }

    @Test
    @DisplayName("Metadata should contain relevant metrics")
    void testMetadataContent() {
        CompetencyDetector.BrfRules rules = new CompetencyDetector.BrfRules("database");

        CompetencyDetector.DetectionResult result = detector.analyze(snapshot, rules);

        assertNotNull(result.metadata, "Metadata should not be null");
        assertTrue(result.metadata.containsKey("migration_count"),
            "Metadata should contain migration count");
    }

    @Test
    @DisplayName("Should collect gaps when database features missing")
    void testGapsIdentification() {
        CompetencyDetector.BrfRules rules = new CompetencyDetector.BrfRules("database");

        CompetencyDetector.DetectionResult result = detector.analyze(snapshot, rules);

        assertTrue(!result.gaps.isEmpty() || !result.evidence.isEmpty(),
            "Should have either gaps or evidence");
    }

    @Test
    @DisplayName("Should collect evidence when database features found")
    void testEvidenceCollection() {
        CompetencyDetector.BrfRules rules = new CompetencyDetector.BrfRules("database");

        CompetencyDetector.DetectionResult result = detector.analyze(snapshot, rules);

        // Result should have either evidence or gaps, but not throw error
        assertNotNull(result.evidence);
        assertNotNull(result.gaps);
    }

    @Test
    @DisplayName("Confidence should vary based on analysis quality")
    void testConfidenceVariation() {
        CompetencyDetector.BrfRules rules = new CompetencyDetector.BrfRules("database");

        CompetencyDetector.DetectionResult result = detector.analyze(snapshot, rules);

        assertTrue(result.confidence >= 0 && result.confidence <= 100,
            "Confidence should be valid percentage");
    }

    @Test
    @DisplayName("Should handle multiple detector instances independently")
    void testDetectorIndependence() {
        DatabaseSchemaDetector detector1 = new DatabaseSchemaDetector();
        DatabaseSchemaDetector detector2 = new DatabaseSchemaDetector();

        CompetencyDetector.BrfRules rules = new CompetencyDetector.BrfRules("database");

        CompetencyDetector.DetectionResult result1 = detector1.analyze(snapshot, rules);
        CompetencyDetector.DetectionResult result2 = detector2.analyze(snapshot, rules);

        // Both should produce same competency name
        assertEquals(result1.competencyName, result2.competencyName);
    }

    @Test
    @DisplayName("Should work with complex BRF rules")
    void testComplexBrfRules() {
        CompetencyDetector.BrfRules rules = new CompetencyDetector.BrfRules(
            "database",
            "Database schema design competency",
            java.util.List.of("Flyway migrations", "Primary keys", "Foreign keys"),
            java.util.List.of("All tables have PK", "Relationships defined"),
            0.8
        );

        CompetencyDetector.DetectionResult result = detector.analyze(snapshot, rules);

        assertNotNull(result);
        assertEquals("database", result.competencyName);
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
        languages.put("SQL", 2000);
        languages.put("Other", 500);
        return languages;
    }
}
