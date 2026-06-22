package com.learnhub.assessment.scoring;

import com.learnhub.assessment.analysis.StaticAnalysisResult;
import com.learnhub.assessment.lm.LLMAnalysisResult;
import com.learnhub.github.entity.RepositorySnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

@DisplayName("CompetencyScoringAlgorithm - Competency Scoring")
class CompetencyScoringAlgorithmTest {

    private CompetencyScoringAlgorithm algorithm;

    @BeforeEach
    void setUp() {
        algorithm = new CompetencyScoringAlgorithm();
    }

    @Test
    @DisplayName("Score is always between 0-100 (no NaN)")
    void testScoreAlwaysValid() {
        StaticAnalysisResult staticResult = new StaticAnalysisResult();
        for (int i = 0; i < 100; i++) {
            staticResult.addFinding("Security", "Critical issue", "CRITICAL", "file" + i);
        }

        LLMAnalysisResult llmResult = new LLMAnalysisResult(
            Map.of("Security", -50),
            Map.of("Security", "Very bad")
        );

        RadarSeriesDto radar = algorithm.computeScores(
            staticResult, llmResult, mockSnapshot()
        );

        for (RadarSeriesDto.RadarSeries series : radar.getSeriesList()) {
            assertThat(series.getValue())
                .isGreaterThanOrEqualTo(0)
                .isLessThanOrEqualTo(100);
        }
    }

    @Test
    @DisplayName("Empty repository (no findings) scores at baseline 70")
    void testEmptyRepositoryBaseline() {
        StaticAnalysisResult staticResult = new StaticAnalysisResult();
        LLMAnalysisResult llmResult = LLMAnalysisResult.DEFAULT;

        RadarSeriesDto radar = algorithm.computeScores(
            staticResult, llmResult, mockSnapshot()
        );

        assertThat(radar.getSeriesList())
            .allMatch(s -> s.getValue() == 70);
    }

    @Test
    @DisplayName("Perfect repository (no findings + perfect LLM) maxes all axes")
    void testPerfectRepositoryMaxes() {
        StaticAnalysisResult staticResult = new StaticAnalysisResult();
        LLMAnalysisResult llmResult = new LLMAnalysisResult(
            Map.of("Security", 30, "Database", 30, "Architecture", 30, "Code Quality", 30),
            Map.of()
        );

        RadarSeriesDto radar = algorithm.computeScores(
            staticResult, llmResult, mockSnapshot()
        );

        assertThat(radar.getSeriesList())
            .allMatch(s -> s.getValue() == 100);
    }

    @Test
    @DisplayName("Critical finding deducts 20 points")
    void testCriticalFindingDeduction() {
        StaticAnalysisResult staticResult = new StaticAnalysisResult();
        staticResult.addFinding("Security", "Hardcoded password", "CRITICAL", "config.properties");

        LLMAnalysisResult llmResult = LLMAnalysisResult.DEFAULT;

        RadarSeriesDto radar = algorithm.computeScores(
            staticResult, llmResult, mockSnapshot()
        );

        RadarSeriesDto.RadarSeries securityAxis = radar.getSeriesList().stream()
            .filter(s -> s.getName().equals("Security"))
            .findFirst()
            .orElseThrow();

        assertThat(securityAxis.getValue()).isEqualTo(50);
    }

    @Test
    @DisplayName("Multiple findings stack correctly")
    void testMultipleFindingsStack() {
        StaticAnalysisResult staticResult = new StaticAnalysisResult();
        staticResult.addFinding("Security", "Issue A", "CRITICAL", "file1.java");
        staticResult.addFinding("Security", "Issue B", "HIGH", "file2.java");
        staticResult.addFinding("Security", "Issue C", "MEDIUM", "file3.java");

        LLMAnalysisResult llmResult = LLMAnalysisResult.DEFAULT;

        RadarSeriesDto radar = algorithm.computeScores(
            staticResult, llmResult, mockSnapshot()
        );

        RadarSeriesDto.RadarSeries securityAxis = radar.getSeriesList().stream()
            .filter(s -> s.getName().equals("Security"))
            .findFirst()
            .orElseThrow();

        // 70 - 20(CRITICAL) - 15(HIGH) - 10(MEDIUM) = 25
        assertThat(securityAxis.getValue()).isEqualTo(25);
    }

    @Test
    @DisplayName("Multiple low-severity findings stack correctly")
    void testMultipleLowSeverityFindings() {
        StaticAnalysisResult staticResult = new StaticAnalysisResult();
        for (int i = 0; i < 5; i++) {
            staticResult.addFinding("Code Quality", "Minor issue " + i, "LOW", "file" + i);
        }

        LLMAnalysisResult llmResult = LLMAnalysisResult.DEFAULT;

        RadarSeriesDto radar = algorithm.computeScores(
            staticResult, llmResult, mockSnapshot()
        );

        RadarSeriesDto.RadarSeries codeQualityAxis = radar.getSeriesList().stream()
            .filter(s -> s.getName().equals("Code Quality"))
            .findFirst()
            .orElseThrow();

        // 70 - (5 * 5 LOW) = 45
        assertThat(codeQualityAxis.getValue()).isEqualTo(45);
    }

    @Test
    @DisplayName("Gap analysis text includes findings")
    void testGapAnalysisIncludesFindings() {
        StaticAnalysisResult staticResult = new StaticAnalysisResult();
        staticResult.addFinding("Security", "SQL Injection Risk", "CRITICAL", "UserRepository.java");
        staticResult.addFinding("Security", "Missing Auth", "HIGH", "AdminController.java");

        LLMAnalysisResult llmResult = LLMAnalysisResult.DEFAULT;

        RadarSeriesDto radar = algorithm.computeScores(
            staticResult, llmResult, mockSnapshot()
        );

        RadarSeriesDto.RadarSeries securityAxis = radar.getSeriesList().stream()
            .filter(s -> s.getName().equals("Security"))
            .findFirst()
            .orElseThrow();

        assertThat(securityAxis.getGapAnalysis())
            .contains("SQL Injection Risk")
            .contains("CRITICAL")
            .contains("UserRepository.java");
    }

    @Test
    @DisplayName("Level badges assigned correctly")
    void testLevelBadgesAssigned() {
        testLevelForScore(90, "EXCELLENT");
        testLevelForScore(70, "GOOD");
        testLevelForScore(50, "FAIR");
        testLevelForScore(20, "POOR");
        testLevelForScore(100, "EXCELLENT");
        testLevelForScore(0, "POOR");
    }

    private void testLevelForScore(int targetScore, String expectedLevel) {
        StaticAnalysisResult staticResult = new StaticAnalysisResult();
        int pointsToDeduct = 70 - targetScore;
        if (pointsToDeduct > 0) {
            int numMedium = pointsToDeduct / 10;
            for (int i = 0; i < numMedium; i++) {
                staticResult.addFinding("Security", "Issue", "MEDIUM", "file");
            }
        }

        LLMAnalysisResult llmResult = LLMAnalysisResult.DEFAULT;

        RadarSeriesDto radar = algorithm.computeScores(
            staticResult, llmResult, mockSnapshot()
        );

        RadarSeriesDto.RadarSeries securityAxis = radar.getSeriesList().stream()
            .filter(s -> s.getName().equals("Security"))
            .findFirst()
            .orElseThrow();

        assertThat(securityAxis.getLevel()).isEqualTo(expectedLevel);
    }

    @Test
    @DisplayName("Score never exceeds 100 even with positive LLM delta")
    void testScoreCappedAt100() {
        StaticAnalysisResult staticResult = new StaticAnalysisResult();
        LLMAnalysisResult llmResult = new LLMAnalysisResult(
            Map.of("Security", 50),
            Map.of()
        );

        RadarSeriesDto radar = algorithm.computeScores(
            staticResult, llmResult, mockSnapshot()
        );

        RadarSeriesDto.RadarSeries securityAxis = radar.getSeriesList().stream()
            .filter(s -> s.getName().equals("Security"))
            .findFirst()
            .orElseThrow();

        assertThat(securityAxis.getValue()).isEqualTo(100);
    }

    @Test
    @DisplayName("Score never goes below 0 even with extreme deductions")
    void testScoreFloorAt0() {
        StaticAnalysisResult staticResult = new StaticAnalysisResult();
        for (int i = 0; i < 20; i++) {
            staticResult.addFinding("Database", "Issue", "CRITICAL", "file");
        }

        LLMAnalysisResult llmResult = new LLMAnalysisResult(
            Map.of("Database", -50),
            Map.of()
        );

        RadarSeriesDto radar = algorithm.computeScores(
            staticResult, llmResult, mockSnapshot()
        );

        RadarSeriesDto.RadarSeries databaseAxis = radar.getSeriesList().stream()
            .filter(s -> s.getName().equals("Database"))
            .findFirst()
            .orElseThrow();

        assertThat(databaseAxis.getValue()).isEqualTo(0);
    }

    @Test
    @DisplayName("All four axes computed correctly")
    void testAllFourAxesComputed() {
        StaticAnalysisResult staticResult = new StaticAnalysisResult();
        staticResult.addFinding("Security", "Issue", "LOW", "file");
        staticResult.addFinding("Database", "Issue", "LOW", "file");
        staticResult.addFinding("Architecture", "Issue", "LOW", "file");
        staticResult.addFinding("Code Quality", "Issue", "LOW", "file");

        LLMAnalysisResult llmResult = LLMAnalysisResult.DEFAULT;

        RadarSeriesDto radar = algorithm.computeScores(
            staticResult, llmResult, mockSnapshot()
        );

        assertThat(radar.getSeriesList()).hasSize(4);
        assertThat(radar.getSeriesList())
            .map(RadarSeriesDto.RadarSeries::getName)
            .containsExactlyInAnyOrder("Security", "Database", "Architecture", "Code Quality");
    }

    private RepositorySnapshot mockSnapshot() {
        List<com.learnhub.github.entity.FileSnapshot> files = new ArrayList<>();
        files.add(new com.learnhub.github.entity.FileSnapshot("App.java", 100, "public class App {}"));

        return new RepositorySnapshot(
            UUID.randomUUID(),
            "Java",
            files,
            Instant.now()
        );
    }
}
