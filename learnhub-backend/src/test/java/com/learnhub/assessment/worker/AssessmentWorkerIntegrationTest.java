package com.learnhub.assessment.worker;

import com.learnhub.assessment.analysis.StaticAnalysisEngine;
import com.learnhub.assessment.entity.Assessment;
import com.learnhub.assessment.entity.AssessmentStatus;
import com.learnhub.assessment.lm.MockLLMAnalysisService;
import com.learnhub.assessment.repository.AssessmentRepository;
import com.learnhub.assessment.scoring.CompetencyScoringAlgorithm;
import com.learnhub.assessment.scoring.RadarSeriesDto;
import com.learnhub.github.entity.FileSnapshot;
import com.learnhub.github.entity.RepositorySnapshot;
import com.learnhub.github.repository.RepositorySnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("AssessmentWorker - Evaluation Engine Integration")
class AssessmentWorkerIntegrationTest {

    @Autowired
    private AssessmentWorker worker;

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private RepositorySnapshotRepository snapshotRepository;

    @Autowired(required = false)
    private MockLLMAnalysisService mockLLM;

    private UUID userId;
    private RepositorySnapshot snapshot;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        // Create test files
        List<FileSnapshot> files = new ArrayList<>();
        files.add(new FileSnapshot("App.java", 100, "public class App { }"));
        files.add(new FileSnapshot("UserService.java", 150, "@Service public class UserService { }"));
        files.add(new FileSnapshot("UserController.java", 80, "@RestController public class UserController { }"));

        // Create snapshot
        snapshot = RepositorySnapshot.builder()
            .userId(userId)
            .githubRepoId(12345L)
            .branch("main")
            .commitSha("abc123def456")
            .filesCount(3)
            .totalSizeKb(50L)
            .languages(java.util.Map.of("Java", 100))
            .filesContent("public class App { } @Service public class UserService { }")
            .files(files)
            .createdAt(LocalDateTime.now())
            .build();

        snapshotRepository.save(snapshot);
    }

    @Test
    @DisplayName("Full pipeline: snapshot → analysis → scoring → COMPLETED storage")
    void testFullEvaluationPipeline() throws InterruptedException {
        // Setup: Create assessment
        Assessment assessment = Assessment.builder()
            .userId(userId)
            .snapshotId(snapshot.getId())
            .snapshot(snapshot)
            .status(AssessmentStatus.PENDING)
            .createdAt(Instant.now())
            .build();

        assessmentRepository.save(assessment);

        // Configure mock LLM if available
        if (mockLLM != null) {
            mockLLM.useFixture("PERFECT_REPO");
        }

        // Execute: Process snapshot
        worker.processAssessmentSnapshot(assessment.getId());

        // Wait for async processing
        Thread.sleep(1500);

        // Verify: Assessment marked COMPLETED
        Assessment completed = assessmentRepository.findById(assessment.getId()).orElseThrow();
        assertThat(completed.getStatus()).isEqualTo(AssessmentStatus.COMPLETED);
        assertThat(completed.getResultsData()).isNotNull();

        // Verify: RadarSeriesDto stored
        RadarSeriesDto radar = completed.getResultsData();
        assertThat(radar.getLanguage()).isEqualTo("Java");
        assertThat(radar.getSeriesList()).hasSize(4);

        // Verify: All scores valid (0-100)
        assertThat(radar.getSeriesList())
            .allMatch(s -> s.getValue() >= 0 && s.getValue() <= 100)
            .allMatch(s -> s.getAxis() != null)
            .allMatch(s -> s.getLevel() != null);
    }

    @Test
    @DisplayName("Security issues detected in analysis")
    void testSecurityAnalysis() throws InterruptedException {
        // Create snapshot with SQL injection vulnerability
        List<FileSnapshot> vulnFiles = new ArrayList<>();
        vulnFiles.add(new FileSnapshot("UserRepository.java", 50,
            "String query = \"SELECT * FROM users WHERE email = '\" + email + \"'\";"));

        RepositorySnapshot vulnSnapshot = RepositorySnapshot.builder()
            .userId(userId)
            .githubRepoId(67890L)
            .branch("main")
            .commitSha("xyz789")
            .filesCount(1)
            .totalSizeKb(10L)
            .languages(java.util.Map.of("Java", 100))
            .filesContent("String query = \"SELECT * FROM users WHERE email = '\" + email + \"'\";")
            .files(vulnFiles)
            .createdAt(LocalDateTime.now())
            .build();

        snapshotRepository.save(vulnSnapshot);

        Assessment assessment = Assessment.builder()
            .userId(userId)
            .snapshotId(vulnSnapshot.getId())
            .snapshot(vulnSnapshot)
            .status(AssessmentStatus.PENDING)
            .createdAt(Instant.now())
            .build();

        assessmentRepository.save(assessment);

        if (mockLLM != null) {
            mockLLM.useFixture("SECURITY_RISK_REPO");
        }

        worker.processAssessmentSnapshot(assessment.getId());
        Thread.sleep(1500);

        Assessment completed = assessmentRepository.findById(assessment.getId()).orElseThrow();
        RadarSeriesDto radar = completed.getResultsData();

        var securityAxis = radar.getSeriesList().stream()
            .filter(s -> s.getName().equals("Security"))
            .findFirst()
            .orElseThrow();

        // Should detect security issues and have gap analysis
        assertThat(securityAxis.getGapAnalysis())
            .isNotEmpty();
        assertThat(securityAxis.getValue())
            .isLessThanOrEqualTo(70);  // Should be penalized
    }

    @Test
    @DisplayName("Architecture issues detected in analysis")
    void testArchitectureAnalysis() throws InterruptedException {
        List<FileSnapshot> archFiles = new ArrayList<>();
        archFiles.add(new FileSnapshot("UserController.java", 200,
            "@RestController @Autowired UserRepository repo; @GetMapping public List<User> get() { return repo.findAll(); }"));

        RepositorySnapshot archSnapshot = RepositorySnapshot.builder()
            .userId(userId)
            .githubRepoId(11111L)
            .branch("main")
            .commitSha("arch123")
            .filesCount(1)
            .totalSizeKb(15L)
            .languages(java.util.Map.of("Java", 100))
            .filesContent("@RestController @Autowired UserRepository repo;")
            .files(archFiles)
            .createdAt(LocalDateTime.now())
            .build();

        snapshotRepository.save(archSnapshot);

        Assessment assessment = Assessment.builder()
            .userId(userId)
            .snapshotId(archSnapshot.getId())
            .snapshot(archSnapshot)
            .status(AssessmentStatus.PENDING)
            .createdAt(Instant.now())
            .build();

        assessmentRepository.save(assessment);

        if (mockLLM != null) {
            mockLLM.useFixture("LEGACY_REPO");
        }

        worker.processAssessmentSnapshot(assessment.getId());
        Thread.sleep(1500);

        Assessment completed = assessmentRepository.findById(assessment.getId()).orElseThrow();
        RadarSeriesDto radar = completed.getResultsData();

        var archAxis = radar.getSeriesList().stream()
            .filter(s -> s.getName().equals("Architecture"))
            .findFirst()
            .orElseThrow();

        // Should detect architecture issues
        assertThat(archAxis.getGapAnalysis())
            .isNotEmpty();
    }

    @Test
    @DisplayName("All four axes computed with valid scores")
    void testAllAxesComputed() throws InterruptedException {
        Assessment assessment = Assessment.builder()
            .userId(userId)
            .snapshotId(snapshot.getId())
            .snapshot(snapshot)
            .status(AssessmentStatus.PENDING)
            .createdAt(Instant.now())
            .build();

        assessmentRepository.save(assessment);

        if (mockLLM != null) {
            mockLLM.useFixture("PERFECT_REPO");
        }

        worker.processAssessmentSnapshot(assessment.getId());
        Thread.sleep(1500);

        Assessment completed = assessmentRepository.findById(assessment.getId()).orElseThrow();
        RadarSeriesDto radar = completed.getResultsData();

        // Verify all 4 axes present
        assertThat(radar.getSeriesList())
            .hasSize(4)
            .extracting(RadarSeriesDto.RadarSeries::getName)
            .containsExactlyInAnyOrder("Security", "Database", "Architecture", "Code Quality");

        // Verify all scores valid
        for (RadarSeriesDto.RadarSeries series : radar.getSeriesList()) {
            assertThat(series.getValue())
                .isBetween(0, 100);
            assertThat(series.getLevel())
                .isIn("EXCELLENT", "GOOD", "FAIR", "POOR");
            assertThat(series.getGapAnalysis())
                .isNotNull();
        }
    }
}
