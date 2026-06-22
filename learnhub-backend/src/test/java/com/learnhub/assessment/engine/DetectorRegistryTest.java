package com.learnhub.assessment.engine;

import com.learnhub.github.entity.RepositorySnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DetectorRegistry.
 *
 * Tests detector discovery, ordering, enable/disable,
 * and registry metadata.
 */
@DisplayName("DetectorRegistry Tests")
class DetectorRegistryTest {

    private DetectorRegistry registry;
    private List<CompetencyDetector> testDetectors;

    @BeforeEach
    void setUp() {
        testDetectors = new ArrayList<>();
        testDetectors.add(new MockDetector("api-design", 10));
        testDetectors.add(new MockDetector("database", 5));
        testDetectors.add(new MockDetector("auth", 15));

        registry = new DetectorRegistryImpl(testDetectors);
    }

    @Test
    @DisplayName("Should auto-discover all detectors")
    void testDetectorDiscovery() {
        assertEquals(3, registry.getDetectorCount(),
            "Should discover all registered detectors");
        assertTrue(registry.hasDetectors(),
            "Should have detectors");
    }

    @Test
    @DisplayName("Should order detectors by priority (high first)")
    void testDetectorOrdering() {
        List<CompetencyDetector> detectors = registry.getDetectors();

        assertEquals(3, detectors.size());
        // Auth has priority 15 (highest)
        assertEquals("auth", detectors.get(0).getCompetencyName());
        // Api-design has priority 10
        assertEquals("api-design", detectors.get(1).getCompetencyName());
        // Database has priority 5 (lowest)
        assertEquals("database", detectors.get(2).getCompetencyName());
    }

    @Test
    @DisplayName("Should find detector by competency name")
    void testGetDetectorByName() {
        CompetencyDetector detector = registry.getDetector("api-design");

        assertNotNull(detector, "Should find detector");
        assertEquals("api-design", detector.getCompetencyName());
    }

    @Test
    @DisplayName("Should return null for unknown detector")
    void testGetUnknownDetector() {
        CompetencyDetector detector = registry.getDetector("unknown");

        assertNull(detector, "Should return null for unknown detector");
    }

    @Test
    @DisplayName("All detectors should be enabled by default")
    void testDefaultEnabled() {
        for (CompetencyDetector detector : testDetectors) {
            assertTrue(registry.isDetectorEnabled(detector.getCompetencyName()),
                "Detector should be enabled by default: " + detector.getCompetencyName());
        }
    }

    @Test
    @DisplayName("Should disable detector")
    void testDisableDetector() {
        registry.disableDetector("api-design");

        assertFalse(registry.isDetectorEnabled("api-design"),
            "Detector should be disabled");
    }

    @Test
    @DisplayName("Should enable detector")
    void testEnableDetector() {
        registry.disableDetector("api-design");
        registry.enableDetector("api-design");

        assertTrue(registry.isDetectorEnabled("api-design"),
            "Detector should be re-enabled");
    }

    @Test
    @DisplayName("Should throw when enabling unknown detector")
    void testEnableUnknownDetector() {
        assertThrows(IllegalArgumentException.class,
            () -> registry.enableDetector("unknown"),
            "Should throw for unknown detector");
    }

    @Test
    @DisplayName("Should throw when disabling unknown detector")
    void testDisableUnknownDetector() {
        assertThrows(IllegalArgumentException.class,
            () -> registry.disableDetector("unknown"),
            "Should throw for unknown detector");
    }

    @Test
    @DisplayName("Should return only enabled detectors")
    void testGetEnabledDetectors() {
        registry.disableDetector("api-design");

        List<CompetencyDetector> enabled = registry.getEnabledDetectors();

        assertEquals(2, enabled.size(), "Should have 2 enabled detectors");
        assertTrue(enabled.stream()
            .noneMatch(d -> "api-design".equals(d.getCompetencyName())),
            "Should not include disabled detector");
    }

    @Test
    @DisplayName("Should set and get detector timeout")
    void testDetectorTimeout() {
        registry.setDetectorTimeout(5000);

        assertEquals(5000, registry.getDetectorTimeout(),
            "Should return set timeout");
    }

    @Test
    @DisplayName("Should throw on invalid timeout")
    void testInvalidTimeout() {
        assertThrows(IllegalArgumentException.class,
            () -> registry.setDetectorTimeout(0),
            "Should reject zero timeout");
        assertThrows(IllegalArgumentException.class,
            () -> registry.setDetectorTimeout(-1),
            "Should reject negative timeout");
    }

    @Test
    @DisplayName("Should have default timeout of 10 seconds")
    void testDefaultTimeout() {
        DetectorRegistry newRegistry = new DetectorRegistryImpl(testDetectors);

        assertEquals(10000, newRegistry.getDetectorTimeout(),
            "Default timeout should be 10 seconds");
    }

    @Test
    @DisplayName("Should provide registry info")
    void testRegistryInfo() {
        DetectorRegistry.DetectorRegistryInfo info = registry.getRegistryInfo();

        assertNotNull(info);
        assertEquals(3, info.getTotalDetectors());
        assertEquals(3, info.getEnabledDetectorCount());
        assertTrue(info.getDetectorNames().contains("api-design"));
    }

    @Test
    @DisplayName("Registry info should reflect disable status")
    void testRegistryInfoAfterDisable() {
        registry.disableDetector("api-design");
        DetectorRegistry.DetectorRegistryInfo info = registry.getRegistryInfo();

        assertEquals(3, info.getTotalDetectors(),
            "Total should still be 3");
        assertEquals(2, info.getEnabledDetectorCount(),
            "Enabled count should be 2");
        assertFalse(info.isEnabled("api-design"),
            "Disabled detector should not be enabled");
    }

    @Test
    @DisplayName("Should get priority from registry info")
    void testGetPriority() {
        DetectorRegistry.DetectorRegistryInfo info = registry.getRegistryInfo();

        assertEquals(10, info.getPriority("api-design"));
        assertEquals(15, info.getPriority("auth"));
    }

    @Test
    @DisplayName("Should throw when querying unknown detector priority")
    void testPriorityForUnknownDetector() {
        DetectorRegistry.DetectorRegistryInfo info = registry.getRegistryInfo();

        assertThrows(IllegalArgumentException.class,
            () -> info.getPriority("unknown"));
    }

    @Test
    @DisplayName("Refresh should not throw error")
    void testRefresh() {
        assertDoesNotThrow(() -> registry.refresh(),
            "Refresh should complete without error");
    }

    @Test
    @DisplayName("Empty detector list should be handled")
    void testEmptyDetectorList() {
        DetectorRegistry emptyRegistry = new DetectorRegistryImpl(new ArrayList<>());

        assertFalse(emptyRegistry.hasDetectors());
        assertEquals(0, emptyRegistry.getDetectorCount());
        assertTrue(emptyRegistry.getDetectors().isEmpty());
        assertTrue(emptyRegistry.getEnabledDetectors().isEmpty());
    }

    /**
     * Mock detector for testing.
     */
    private static class MockDetector implements CompetencyDetector {
        private final String competencyName;
        private final int priority;

        MockDetector(String competencyName, int priority) {
            this.competencyName = competencyName;
            this.priority = priority;
        }

        @Override
        public String getCompetencyName() {
            return competencyName;
        }

        @Override
        public DetectionResult analyze(RepositorySnapshot snapshot, BrfRules rules) {
            return new DetectionResult(competencyName, CompetencyLevel.NOT_DEMONSTRATED);
        }

        @Override
        public int getPriority() {
            return priority;
        }
    }
}
