package com.learnhub.assessment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnhub.assessment.dto.AssessmentDetailsResponse;
import com.learnhub.assessment.dto.SubmitAssessmentRequest;
import com.learnhub.assessment.entity.Assessment;
import com.learnhub.assessment.entity.AssessmentResult;
import com.learnhub.assessment.entity.AssessmentStatus;
import com.learnhub.assessment.engine.CompetencyScoringEngine;
import com.learnhub.assessment.repository.AssessmentRepository;
import com.learnhub.assessment.repository.AssessmentResultRepository;
import com.learnhub.assessment.service.AssessmentService;
import com.learnhub.github.entity.RepositorySnapshot;
import com.learnhub.github.repository.RepositorySnapshotRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration tests for Phase 3 (API & Scoring Layer).
 *
 * Tests the complete flow:
 * 1. Submit assessment via REST API
 * 2. Service persists assessment
 * 3. Verify assessment status and results are accessible
 * 4. Verify assessment result entity persisted correctly
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Slf4j
class AssessmentPhase3IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AssessmentService assessmentService;

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private AssessmentResultRepository resultRepository;

    @Autowired
    private RepositorySnapshotRepository snapshotRepository;

    private UUID testUserId;
    private UUID testSnapshotId;
    private RepositorySnapshot testSnapshot;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testSnapshotId = UUID.randomUUID();

        // Create test snapshot
        testSnapshot = RepositorySnapshot.builder()
                .id(testSnapshotId)
                .userId(testUserId)
                .repositoryId(UUID.randomUUID())
                .ownerUsername("test-owner")
                .repositoryName("test-repo")
                .branchName("main")
                .commitSha("abc123def456")
                .filesContent(new HashMap<>())
                .createdAt(Instant.now())
                .build();

        snapshotRepository.save(testSnapshot);
    }

    // ========== API Endpoint Tests ==========

    @Test
    @WithMockUser(roles = "LEARNER")
    void testSubmitAssessment_Creates_PendingAssessment() throws Exception {
        SubmitAssessmentRequest request = new SubmitAssessmentRequest(testSnapshotId);

        MvcResult result = mockMvc.perform(post("/api/v1/assessments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.snapshotId").value(testSnapshotId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        // Verify assessment persisted with PENDING status
        String responseBody = result.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
        UUID assessmentId = UUID.fromString((String) responseMap.get("id"));

        Optional<Assessment> savedAssessment = assessmentRepository.findById(assessmentId);
        assertTrue(savedAssessment.isPresent());
        assertEquals(AssessmentStatus.PENDING, savedAssessment.get().getStatus());
        assertEquals(testSnapshotId, savedAssessment.get().getSnapshotId());
    }

    @Test
    @WithMockUser(roles = "LEARNER")
    void testGetAssessment_Returns_Assessment_Status() throws Exception {
        // Create and persist an assessment
        Assessment assessment = Assessment.builder()
                .userId(testUserId)
                .snapshotId(testSnapshotId)
                .status(AssessmentStatus.PENDING)
                .build();
        Assessment saved = assessmentRepository.save(assessment);

        // Fetch via REST API
        mockMvc.perform(get("/api/v1/assessments/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.snapshotId").value(testSnapshotId.toString()));
    }

    @Test
    @WithMockUser(roles = "LEARNER")
    void testGetAssessmentDetails_Returns_Results_When_Completed() throws Exception {
        // Create assessment
        Assessment assessment = Assessment.builder()
                .userId(testUserId)
                .snapshotId(testSnapshotId)
                .status(AssessmentStatus.COMPLETED)
                .completedAt(Instant.now())
                .build();
        Assessment savedAssessment = assessmentRepository.save(assessment);

        // Create assessment result
        AssessmentResult result = AssessmentResult.builder()
                .assessmentId(savedAssessment.getId())
                .overallLevel("PROFICIENT")
                .allGaps(List.of("Gap 1", "Gap 2"))
                .nextSteps(List.of("Step 1", "Step 2"))
                .overallConfidence(0.85)
                .resultsJson("{}")
                .build();
        resultRepository.save(result);

        // Fetch details via REST API
        mockMvc.perform(get("/api/v1/assessments/" + savedAssessment.getId() + "/details"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedAssessment.getId().toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.result.overallLevel").value("PROFICIENT"))
                .andExpect(jsonPath("$.result.allGaps[0]").value("Gap 1"))
                .andExpect(jsonPath("$.result.nextSteps[0]").value("Step 1"))
                .andExpect(jsonPath("$.result.overallConfidence").value(0.85));
    }

    @Test
    @WithMockUser(roles = "LEARNER")
    void testGetUserAssessments_Returns_All_User_Assessments() throws Exception {
        // Create multiple assessments for user
        Assessment assessment1 = Assessment.builder()
                .userId(testUserId)
                .snapshotId(testSnapshotId)
                .status(AssessmentStatus.PENDING)
                .build();
        Assessment assessment2 = Assessment.builder()
                .userId(testUserId)
                .snapshotId(UUID.randomUUID())
                .status(AssessmentStatus.COMPLETED)
                .completedAt(Instant.now())
                .build();
        assessmentRepository.save(assessment1);
        assessmentRepository.save(assessment2);

        // Fetch all assessments
        mockMvc.perform(get("/api/v1/assessments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assessments.length()").value(2))
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    @WithMockUser(roles = "LEARNER")
    void testGetAssessment_Returns_404_For_Nonexistent_Assessment() throws Exception {
        UUID nonexistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/assessments/" + nonexistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "LEARNER")
    void testGetAssessmentDetails_Returns_404_For_Nonexistent_Assessment() throws Exception {
        UUID nonexistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/assessments/" + nonexistentId + "/details"))
                .andExpect(status().isNotFound());
    }

    // ========== Service Layer Tests ==========

    @Test
    void testAssessmentService_SavesAssessmentResult() {
        // Create assessment
        Assessment assessment = Assessment.builder()
                .userId(testUserId)
                .snapshotId(testSnapshotId)
                .status(AssessmentStatus.COMPLETED)
                .completedAt(Instant.now())
                .build();
        Assessment savedAssessment = assessmentRepository.save(assessment);

        // Save result
        AssessmentResult result = AssessmentResult.builder()
                .overallLevel("PROFICIENT")
                .allGaps(List.of("Gap 1"))
                .nextSteps(List.of("Step 1"))
                .overallConfidence(0.85)
                .build();
        AssessmentResult savedResult = assessmentService.saveAssessmentResult(
                savedAssessment.getId(), result);

        // Verify
        assertNotNull(savedResult.getId());
        assertEquals(savedAssessment.getId(), savedResult.getAssessmentId());
        assertEquals("PROFICIENT", savedResult.getOverallLevel());
    }

    @Test
    void testAssessmentService_GetAssessmentResult() {
        // Create assessment and result
        Assessment assessment = Assessment.builder()
                .userId(testUserId)
                .snapshotId(testSnapshotId)
                .status(AssessmentStatus.COMPLETED)
                .completedAt(Instant.now())
                .build();
        Assessment savedAssessment = assessmentRepository.save(assessment);

        AssessmentResult result = AssessmentResult.builder()
                .assessmentId(savedAssessment.getId())
                .overallLevel("EMERGING")
                .allGaps(List.of("Gap 1"))
                .nextSteps(List.of("Step 1"))
                .overallConfidence(0.65)
                .build();
        resultRepository.save(result);

        // Retrieve via service
        Optional<AssessmentResult> retrieved =
                assessmentService.getAssessmentResult(savedAssessment.getId());

        assertTrue(retrieved.isPresent());
        assertEquals("EMERGING", retrieved.get().getOverallLevel());
    }

    @Test
    void testAssessmentService_GetAssessmentWithResult_VerifiesOwnership() {
        // Create assessment for different user
        UUID otherUserId = UUID.randomUUID();
        Assessment assessment = Assessment.builder()
                .userId(otherUserId)
                .snapshotId(testSnapshotId)
                .status(AssessmentStatus.COMPLETED)
                .completedAt(Instant.now())
                .build();
        Assessment savedAssessment = assessmentRepository.save(assessment);

        // Try to retrieve as different user - should fail
        assertThrows(
                RuntimeException.class,
                () -> assessmentService.getAssessmentWithResult(
                        savedAssessment.getId(), testUserId)
        );
    }

    // ========== Database Persistence Tests ==========

    @Test
    void testAssessmentResultEntity_PersistsAllFields() {
        Assessment assessment = Assessment.builder()
                .userId(testUserId)
                .snapshotId(testSnapshotId)
                .status(AssessmentStatus.COMPLETED)
                .completedAt(Instant.now())
                .build();
        Assessment savedAssessment = assessmentRepository.save(assessment);

        List<String> gaps = List.of("Missing error handling", "Incomplete tests");
        List<String> steps = List.of("Review error patterns", "Add unit tests");

        AssessmentResult result = AssessmentResult.builder()
                .assessmentId(savedAssessment.getId())
                .overallLevel("PROFICIENT")
                .allGaps(gaps)
                .nextSteps(steps)
                .overallConfidence(0.85)
                .resultsJson("{\"test\": \"data\"}")
                .build();
        AssessmentResult saved = resultRepository.save(result);

        // Retrieve and verify
        Optional<AssessmentResult> retrieved = resultRepository.findById(saved.getId());
        assertTrue(retrieved.isPresent());

        AssessmentResult persisted = retrieved.get();
        assertEquals(savedAssessment.getId(), persisted.getAssessmentId());
        assertEquals("PROFICIENT", persisted.getOverallLevel());
        assertEquals(2, persisted.getAllGaps().size());
        assertEquals(2, persisted.getNextSteps().size());
        assertEquals(0.85, persisted.getOverallConfidence());
        assertTrue(persisted.getResultsJson().contains("test"));
        assertNotNull(persisted.getCreatedAt());
    }

    @Test
    void testAssessmentResultRepository_FindByAssessmentId() {
        Assessment assessment = Assessment.builder()
                .userId(testUserId)
                .snapshotId(testSnapshotId)
                .status(AssessmentStatus.COMPLETED)
                .completedAt(Instant.now())
                .build();
        Assessment savedAssessment = assessmentRepository.save(assessment);

        AssessmentResult result = AssessmentResult.builder()
                .assessmentId(savedAssessment.getId())
                .overallLevel("ADVANCED")
                .allGaps(new ArrayList<>())
                .nextSteps(List.of("Mentor junior developers"))
                .overallConfidence(0.95)
                .build();
        resultRepository.save(result);

        // Retrieve by assessment ID
        Optional<AssessmentResult> found =
                resultRepository.findByAssessmentId(savedAssessment.getId());

        assertTrue(found.isPresent());
        assertEquals("ADVANCED", found.get().getOverallLevel());
    }

    @Test
    void testOneToOne_AssessmentToResult_Relationship() {
        Assessment assessment = Assessment.builder()
                .userId(testUserId)
                .snapshotId(testSnapshotId)
                .status(AssessmentStatus.COMPLETED)
                .completedAt(Instant.now())
                .build();
        Assessment savedAssessment = assessmentRepository.save(assessment);

        AssessmentResult result1 = AssessmentResult.builder()
                .assessmentId(savedAssessment.getId())
                .overallLevel("PROFICIENT")
                .allGaps(List.of("Gap"))
                .nextSteps(List.of("Step"))
                .overallConfidence(0.8)
                .build();
        resultRepository.save(result1);

        // Try to save another result for same assessment - should fail due to UNIQUE constraint
        AssessmentResult result2 = AssessmentResult.builder()
                .assessmentId(savedAssessment.getId())
                .overallLevel("EMERGING")
                .allGaps(List.of("Gap"))
                .nextSteps(List.of("Step"))
                .overallConfidence(0.6)
                .build();

        assertThrows(
                Exception.class,
                () -> resultRepository.save(result2)
        );
    }

    // ========== Status Transition Tests ==========

    @Test
    void testAssessmentStatusTransition_PendingToCompleted() {
        Assessment assessment = Assessment.builder()
                .userId(testUserId)
                .snapshotId(testSnapshotId)
                .status(AssessmentStatus.PENDING)
                .build();
        Assessment saved = assessmentRepository.save(assessment);
        assertNull(saved.getCompletedAt());

        // Transition to COMPLETED
        Assessment updated = assessmentService.updateStatus(
                saved.getId(), AssessmentStatus.COMPLETED);

        assertEquals(AssessmentStatus.COMPLETED, updated.getStatus());
        assertNotNull(updated.getCompletedAt());
    }

    @Test
    void testAssessmentStatusTransition_PendingToProcessing() {
        Assessment assessment = Assessment.builder()
                .userId(testUserId)
                .snapshotId(testSnapshotId)
                .status(AssessmentStatus.PENDING)
                .build();
        Assessment saved = assessmentRepository.save(assessment);

        // Transition to PROCESSING
        Assessment updated = assessmentService.updateStatus(
                saved.getId(), AssessmentStatus.PROCESSING);

        assertEquals(AssessmentStatus.PROCESSING, updated.getStatus());
    }

    @Test
    void testAssessmentStatusTransition_ProcessingToFailed() {
        Assessment assessment = Assessment.builder()
                .userId(testUserId)
                .snapshotId(testSnapshotId)
                .status(AssessmentStatus.PROCESSING)
                .startedAt(Instant.now())
                .build();
        Assessment saved = assessmentRepository.save(assessment);

        // Transition to FAILED with error message
        Assessment updated = assessmentService.updateStatus(
                saved.getId(), AssessmentStatus.FAILED);
        updated.setErrorMessage("Detector timeout");
        assessmentRepository.save(updated);

        assertEquals(AssessmentStatus.FAILED, updated.getStatus());
        assertNotNull(updated.getCompletedAt());
        assertEquals("Detector timeout", updated.getErrorMessage());
    }

    // ========== Error Handling Tests ==========

    @Test
    void testAssessmentService_SaveResult_WithNullAssessmentId_Throws() {
        AssessmentResult result = AssessmentResult.builder()
                .overallLevel("PROFICIENT")
                .allGaps(List.of())
                .nextSteps(List.of())
                .overallConfidence(0.8)
                .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> assessmentService.saveAssessmentResult(null, result)
        );
    }

    @Test
    void testAssessmentService_SaveResult_WithNullResult_Throws() {
        Assessment assessment = Assessment.builder()
                .userId(testUserId)
                .snapshotId(testSnapshotId)
                .status(AssessmentStatus.COMPLETED)
                .completedAt(Instant.now())
                .build();
        Assessment saved = assessmentRepository.save(assessment);

        assertThrows(
                IllegalArgumentException.class,
                () -> assessmentService.saveAssessmentResult(saved.getId(), null)
        );
    }

    @Test
    void testAssessmentService_SaveResult_WithNonexistentAssessment_Throws() {
        AssessmentResult result = AssessmentResult.builder()
                .overallLevel("PROFICIENT")
                .allGaps(List.of())
                .nextSteps(List.of())
                .overallConfidence(0.8)
                .build();

        assertThrows(
                RuntimeException.class,
                () -> assessmentService.saveAssessmentResult(UUID.randomUUID(), result)
        );
    }
}
