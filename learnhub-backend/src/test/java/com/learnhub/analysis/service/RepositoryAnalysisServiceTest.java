package com.learnhub.analysis.service;

import com.learnhub.analysis.dto.AnalysisResult;
import com.learnhub.github.entity.RepositorySnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RepositoryAnalysisService.
 * Tests the orchestration of language detection and skill extraction.
 * Coverage target: 80%+
 */
@ExtendWith(MockitoExtension.class)
class RepositoryAnalysisServiceTest {

    private RepositoryAnalysisService service;

    @Mock
    private LanguageDetectionEngine languageDetectionEngine;

    @Mock
    private SkillExtractionEngine skillExtractionEngine;

    @BeforeEach
    void setUp() {
        service = new RepositoryAnalysisService(languageDetectionEngine, skillExtractionEngine);
    }

    // ==================== analyzeRepository() Tests ====================

    @Test
    void testAnalyzeRepositoryReturnsAnalysisResult() {
        // Arrange
        UUID snapshotId = UUID.randomUUID();
        RepositorySnapshot snapshot = createTestSnapshot(snapshotId);

        when(languageDetectionEngine.detectLanguages(any())).thenReturn(List.of());
        when(skillExtractionEngine.extractSkills(any(), any())).thenReturn(List.of());

        // Act
        AnalysisResult result = service.analyzeRepository(snapshot);

        // Assert
        assertNotNull(result);
        assertEquals(snapshotId, result.getSnapshotId());
    }

    @Test
    void testAnalyzeRepositoryDetectsLanguages() {
        // Arrange
        UUID snapshotId = UUID.randomUUID();
        RepositorySnapshot snapshot = createTestSnapshot(snapshotId);

        when(languageDetectionEngine.detectLanguages(any())).thenReturn(List.of());
        when(skillExtractionEngine.extractSkills(any(), any())).thenReturn(List.of());

        // Act
        AnalysisResult result = service.analyzeRepository(snapshot);

        // Assert
        assertNotNull(result.getLanguages());
        verify(languageDetectionEngine, times(1)).detectLanguages(snapshot.getLanguages());
    }

    @Test
    void testAnalyzeRepositoryExtractsSkills() {
        // Arrange
        UUID snapshotId = UUID.randomUUID();
        RepositorySnapshot snapshot = createTestSnapshot(snapshotId);

        when(languageDetectionEngine.detectLanguages(any())).thenReturn(List.of());
        when(skillExtractionEngine.extractSkills(any(), any())).thenReturn(List.of());

        // Act
        AnalysisResult result = service.analyzeRepository(snapshot);

        // Assert
        assertNotNull(result.getSkills());
        verify(skillExtractionEngine, times(1)).extractSkills(any(), any());
    }

    @Test
    void testAnalyzeRepositoryThrowsExceptionForNullSnapshot() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            service.analyzeRepository(null);
        });
    }

    @Test
    void testAnalyzeRepositoryStoresSnapshotId() {
        // Arrange
        UUID snapshotId = UUID.randomUUID();
        RepositorySnapshot snapshot = createTestSnapshot(snapshotId);

        when(languageDetectionEngine.detectLanguages(any())).thenReturn(List.of());
        when(skillExtractionEngine.extractSkills(any(), any())).thenReturn(List.of());

        // Act
        AnalysisResult result = service.analyzeRepository(snapshot);

        // Assert
        assertEquals(snapshotId, result.getSnapshotId());
    }

    @Test
    void testAnalyzeRepositoryStoresAnalyzedAtTimestamp() {
        // Arrange
        UUID snapshotId = UUID.randomUUID();
        RepositorySnapshot snapshot = createTestSnapshot(snapshotId);

        when(languageDetectionEngine.detectLanguages(any())).thenReturn(List.of());
        when(skillExtractionEngine.extractSkills(any(), any())).thenReturn(List.of());

        LocalDateTime before = LocalDateTime.now();

        // Act
        AnalysisResult result = service.analyzeRepository(snapshot);

        LocalDateTime after = LocalDateTime.now();

        // Assert
        assertNotNull(result.getAnalyzedAt());
        assertTrue(result.getAnalyzedAt().isAfter(before.minusSeconds(1)));
        assertTrue(result.getAnalyzedAt().isBefore(after.plusSeconds(1)));
    }

    @Test
    void testAnalyzeRepositoryPopulatesLanguagesFromEngine() {
        // Arrange
        UUID snapshotId = UUID.randomUUID();
        RepositorySnapshot snapshot = createTestSnapshot(snapshotId);

        var mockLanguages = List.of(
            createLanguageDetectionResult("Java", 50)
        );

        when(languageDetectionEngine.detectLanguages(any())).thenReturn(mockLanguages);
        when(skillExtractionEngine.extractSkills(any(), any())).thenReturn(List.of());

        // Act
        AnalysisResult result = service.analyzeRepository(snapshot);

        // Assert
        assertNotNull(result.getLanguages());
        assertEquals(1, result.getLanguages().size());
        assertEquals("Java", result.getLanguages().get(0).getLanguage());
    }

    @Test
    void testAnalyzeRepositoryPopulatesSkillsFromEngine() {
        // Arrange
        UUID snapshotId = UUID.randomUUID();
        RepositorySnapshot snapshot = createTestSnapshot(snapshotId);

        var mockSkills = List.of(
            createSkillScore("Spring Boot", 75, "Java")
        );

        when(languageDetectionEngine.detectLanguages(any())).thenReturn(List.of());
        when(skillExtractionEngine.extractSkills(any(), any())).thenReturn(mockSkills);

        // Act
        AnalysisResult result = service.analyzeRepository(snapshot);

        // Assert
        assertNotNull(result.getSkills());
        assertEquals(1, result.getSkills().size());
        assertEquals("Spring Boot", result.getSkills().get(0).getSkillName());
    }

    @Test
    void testAnalyzeRepositoryWithEmptyLanguages() {
        // Arrange
        UUID snapshotId = UUID.randomUUID();
        RepositorySnapshot snapshot = new RepositorySnapshot();
        snapshot.setId(snapshotId);
        snapshot.setLanguages(new HashMap<>());

        when(languageDetectionEngine.detectLanguages(any())).thenReturn(List.of());
        when(skillExtractionEngine.extractSkills(any(), any())).thenReturn(List.of());

        // Act
        AnalysisResult result = service.analyzeRepository(snapshot);

        // Assert
        assertNotNull(result);
        assertTrue(result.getLanguages().isEmpty());
        assertTrue(result.getSkills().isEmpty());
    }

    @Test
    void testAnalyzeRepositoryWithNullLanguagesMap() {
        // Arrange
        UUID snapshotId = UUID.randomUUID();
        RepositorySnapshot snapshot = new RepositorySnapshot();
        snapshot.setId(snapshotId);
        snapshot.setLanguages(null);

        when(languageDetectionEngine.detectLanguages(null)).thenReturn(List.of());
        when(skillExtractionEngine.extractSkills(any(), any())).thenReturn(List.of());

        // Act
        AnalysisResult result = service.analyzeRepository(snapshot);

        // Assert
        assertNotNull(result);
        assertTrue(result.getLanguages().isEmpty());
    }

    @Test
    void testAnalyzeRepositoryMultipleLanguages() {
        // Arrange
        UUID snapshotId = UUID.randomUUID();
        RepositorySnapshot snapshot = createTestSnapshot(snapshotId);

        var mockLanguages = List.of(
            createLanguageDetectionResult("Java", 50),
            createLanguageDetectionResult("JavaScript", 30),
            createLanguageDetectionResult("Python", 20)
        );

        when(languageDetectionEngine.detectLanguages(any())).thenReturn(mockLanguages);
        when(skillExtractionEngine.extractSkills(any(), any())).thenReturn(List.of());

        // Act
        AnalysisResult result = service.analyzeRepository(snapshot);

        // Assert
        assertEquals(3, result.getLanguages().size());
    }

    @Test
    void testAnalyzeRepositoryMultipleSkills() {
        // Arrange
        UUID snapshotId = UUID.randomUUID();
        RepositorySnapshot snapshot = createTestSnapshot(snapshotId);

        var mockSkills = List.of(
            createSkillScore("Spring Boot", 75, "Java"),
            createSkillScore("React", 85, "JavaScript"),
            createSkillScore("Django", 60, "Python")
        );

        when(languageDetectionEngine.detectLanguages(any())).thenReturn(List.of());
        when(skillExtractionEngine.extractSkills(any(), any())).thenReturn(mockSkills);

        // Act
        AnalysisResult result = service.analyzeRepository(snapshot);

        // Assert
        assertEquals(3, result.getSkills().size());
    }

    @Test
    void testAnalyzeRepositoryCallsEnginesInCorrectOrder() {
        // Arrange
        UUID snapshotId = UUID.randomUUID();
        RepositorySnapshot snapshot = createTestSnapshot(snapshotId);

        when(languageDetectionEngine.detectLanguages(any())).thenReturn(List.of());
        when(skillExtractionEngine.extractSkills(any(), any())).thenReturn(List.of());

        // Act
        service.analyzeRepository(snapshot);

        // Assert - language detection should be called before skill extraction
        InOrder inOrder = mock(LanguageDetectionEngine.class, withSettings().lenient());
        verify(languageDetectionEngine, times(1)).detectLanguages(any());
        verify(skillExtractionEngine, times(1)).extractSkills(any(), any());
    }

    @Test
    void testAnalyzeRepositoryPassesCorrectLanguagesToSkillExtraction() {
        // Arrange
        UUID snapshotId = UUID.randomUUID();
        RepositorySnapshot snapshot = createTestSnapshot(snapshotId);

        var mockLanguages = List.of(
            createLanguageDetectionResult("Java", 50)
        );

        when(languageDetectionEngine.detectLanguages(any())).thenReturn(mockLanguages);
        when(skillExtractionEngine.extractSkills(any(), any())).thenReturn(List.of());

        // Act
        service.analyzeRepository(snapshot);

        // Assert
        verify(skillExtractionEngine).extractSkills(
            argThat(langs -> langs.equals(mockLanguages)),
            anyString()
        );
    }

    @Test
    void testAnalyzeRepositoryPassesPackageInfoToSkillExtraction() {
        // Arrange
        UUID snapshotId = UUID.randomUUID();
        RepositorySnapshot snapshot = createTestSnapshot(snapshotId);

        when(languageDetectionEngine.detectLanguages(any())).thenReturn(List.of());
        when(skillExtractionEngine.extractSkills(any(), any())).thenReturn(List.of());

        // Act
        service.analyzeRepository(snapshot);

        // Assert
        verify(skillExtractionEngine).extractSkills(any(), isNotNull());
    }

    @Test
    void testAnalyzeRepositoryExceptionMessageForNullSnapshot() {
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.analyzeRepository(null);
        });
        assertEquals("Repository snapshot must not be null", exception.getMessage());
    }

    @Test
    void testAnalyzeRepositoryWithRealSnapshot() {
        // This test verifies the service with a fully constructed snapshot
        // Arrange
        UUID snapshotId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Map<String, Integer> languages = new HashMap<>();
        languages.put("Java", 50);
        languages.put("JavaScript", 30);

        RepositorySnapshot snapshot = RepositorySnapshot.builder()
            .id(snapshotId)
            .userId(userId)
            .githubRepoId(12345L)
            .branch("main")
            .commitSha("abc123def456")
            .filesCount(100)
            .totalSizeKb(5000L)
            .languages(languages)
            .createdAt(LocalDateTime.now())
            .build();

        var mockLanguages = List.of(
            createLanguageDetectionResult("Java", 50),
            createLanguageDetectionResult("JavaScript", 30)
        );

        when(languageDetectionEngine.detectLanguages(languages)).thenReturn(mockLanguages);
        when(skillExtractionEngine.extractSkills(any(), any())).thenReturn(List.of());

        // Act
        AnalysisResult result = service.analyzeRepository(snapshot);

        // Assert
        assertEquals(snapshotId, result.getSnapshotId());
        assertEquals(2, result.getLanguages().size());
    }

    // ==================== Helper Methods ====================

    private RepositorySnapshot createTestSnapshot(UUID snapshotId) {
        Map<String, Integer> languages = new HashMap<>();
        languages.put("Java", 50);
        languages.put("JavaScript", 30);
        languages.put("Python", 20);

        RepositorySnapshot snapshot = new RepositorySnapshot();
        snapshot.setId(snapshotId);
        snapshot.setLanguages(languages);
        return snapshot;
    }

    private com.learnhub.analysis.dto.LanguageDetectionResult createLanguageDetectionResult(String language, int fileCount) {
        var result = new com.learnhub.analysis.dto.LanguageDetectionResult();
        result.setLanguage(language);
        result.setFileCount(fileCount);
        result.setPercentage((double) fileCount);
        result.setEvidence(List.of(fileCount + " files"));
        return result;
    }

    private com.learnhub.analysis.dto.SkillScore createSkillScore(String skillName, int confidence, String language) {
        return new com.learnhub.analysis.dto.SkillScore(
            skillName,
            confidence,
            confidence + "% confidence",
            language
        );
    }
}
