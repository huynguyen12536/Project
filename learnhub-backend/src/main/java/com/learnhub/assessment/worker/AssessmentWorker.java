package com.learnhub.assessment.worker;

import com.learnhub.assessment.analysis.StaticAnalysisEngine;
import com.learnhub.assessment.dto.AssessmentProgressEvent;
import com.learnhub.assessment.entity.Assessment;
import com.learnhub.assessment.entity.AssessmentStatus;
import com.learnhub.assessment.exception.AssessmentNotFoundException;
import com.learnhub.assessment.lm.LLMAnalysisService;
import com.learnhub.assessment.lm.LLMAnalysisResult;
import com.learnhub.assessment.repository.AssessmentRepository;
import com.learnhub.assessment.scoring.CompetencyScoringAlgorithm;
import com.learnhub.assessment.util.SseEmitterManager;
import com.learnhub.github.entity.RepositorySnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@Slf4j
public class AssessmentWorker {

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private CompetencyScoringAlgorithm scoringAlgorithm;

    @Autowired
    private SseEmitterManager sseManager;

    @Autowired
    private StaticAnalysisEngine staticAnalysisEngine;

    @Autowired
    private LLMAnalysisService llmAnalysisService;

    @Async("assessmentExecutor")
    public void processAssessmentSnapshot(UUID assessmentId) {
        Assessment assessment = null;
        try {
            assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new AssessmentNotFoundException(assessmentId));

            RepositorySnapshot snapshot = assessment.getSnapshot();
            UUID userId = assessment.getUserId();

            updateProgress(assessment, AssessmentStatus.PROCESSING, 10, "Initializing analysis");

            // Step 2: Static analysis (fast, 100-500ms)
            var staticResult = staticAnalysisEngine.analyze(snapshot);
            updateProgress(assessment, AssessmentStatus.PROCESSING, 35, "Completed syntax analysis");

            // Step 3: LLM deep analysis (slower, 2-5s)
            LLMAnalysisResult llmResult = llmAnalysisService.analyzeRepository(snapshot);
            updateProgress(assessment, AssessmentStatus.PROCESSING, 70, "Architectural evaluation complete");

            // Step 4: Compute scores using hybrid results
            var radarData = scoringAlgorithm.computeScores(staticResult, llmResult, snapshot);
            updateProgress(assessment, AssessmentStatus.PROCESSING, 85, "Scoring competencies");

            // Step 5: Serialize to JSONB, store in DB
            assessment.setStatus(AssessmentStatus.COMPLETED);
            assessment.setResultsData(radarData);
            assessment.setCompletedAt(Instant.now());
            assessmentRepository.save(assessment);

            updateProgress(assessment, AssessmentStatus.COMPLETED, 100, "Analysis complete");

            log.info("Assessment {} completed successfully", assessmentId);

        } catch (Exception e) {
            log.error("Assessment {} failed: {}", assessmentId, e.getMessage(), e);
            if (assessment != null) {
                assessment.setStatus(AssessmentStatus.FAILED);
                assessmentRepository.save(assessment);
                updateProgress(assessment, AssessmentStatus.FAILED, 0, "Error: " + e.getMessage());
            }
        }
    }

    private void updateProgress(Assessment assessment, AssessmentStatus status, int progress, String step) {
        AssessmentProgressEvent event = new AssessmentProgressEvent(
            assessment.getId(),
            status,
            progress,
            step,
            estimateRemainingSeconds(progress),
            null,
            confidenceScore(progress),
            Instant.now()
        );

        sseManager.emit(assessment.getId(), event);
    }

    private Integer estimateRemainingSeconds(int progress) {
        return Math.max(0, (100 - progress) / 12);
    }

    private Double confidenceScore(int progress) {
        return Math.min(1.0, progress / 100.0);
    }
}
