package com.learnhub.assessment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnhub.assessment.dto.SubmitAssessmentRequest;
import com.learnhub.assessment.entity.Assessment;
import com.learnhub.assessment.entity.AssessmentStatus;
import com.learnhub.assessment.exception.AssessmentNotFoundException;
import com.learnhub.assessment.service.AssessmentService;
import com.learnhub.common.util.AuthenticationUtil;
import com.learnhub.github.exception.SnapshotNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AssessmentController.class)
class AssessmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AssessmentService assessmentService;

    @MockBean
    private AuthenticationUtil authenticationUtil;

    private UUID testUserId;
    private UUID testSnapshotId;
    private UUID testAssessmentId;
    private Assessment testAssessment;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testSnapshotId = UUID.randomUUID();
        testAssessmentId = UUID.randomUUID();

        testAssessment = Assessment.builder()
            .id(testAssessmentId)
            .userId(testUserId)
            .snapshotId(testSnapshotId)
            .status(AssessmentStatus.PENDING)
            .createdAt(LocalDateTime.now())
            .build();
    }

    // ========== submitAssessment() Tests ==========

    @Test
    @WithMockUser(roles = "LEARNER")
    void testSubmitAssessment_Success() throws Exception {
        SubmitAssessmentRequest request = new SubmitAssessmentRequest(testSnapshotId);

        when(authenticationUtil.getCurrentUserId()).thenReturn(testUserId);
        when(assessmentService.submitAssessment(testUserId, testSnapshotId))
            .thenReturn(testAssessment);

        mockMvc.perform(post("/api/v1/assessments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", notNullValue()))
            .andExpect(jsonPath("$.userId", is(testUserId.toString())))
            .andExpect(jsonPath("$.snapshotId", is(testSnapshotId.toString())))
            .andExpect(jsonPath("$.status", is("PENDING")));

        verify(authenticationUtil, times(1)).getCurrentUserId();
        verify(assessmentService, times(1)).submitAssessment(testUserId, testSnapshotId);
    }

    @Test
    @WithMockUser(roles = "LEARNER")
    void testSubmitAssessment_InvalidRequest_NullSnapshotId() throws Exception {
        SubmitAssessmentRequest request = new SubmitAssessmentRequest(null);

        when(authenticationUtil.getCurrentUserId()).thenReturn(testUserId);

        mockMvc.perform(post("/api/v1/assessments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(assessmentService, never()).submitAssessment(any(), any());
    }

    @Test
    @WithMockUser(roles = "LEARNER")
    void testSubmitAssessment_SnapshotNotFound() throws Exception {
        SubmitAssessmentRequest request = new SubmitAssessmentRequest(testSnapshotId);

        when(authenticationUtil.getCurrentUserId()).thenReturn(testUserId);
        when(assessmentService.submitAssessment(testUserId, testSnapshotId))
            .thenThrow(new SnapshotNotFoundException("Snapshot not found or not owned by user"));

        mockMvc.perform(post("/api/v1/assessments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());

        verify(assessmentService, times(1)).submitAssessment(testUserId, testSnapshotId);
    }

    @Test
    void testSubmitAssessment_Unauthorized() throws Exception {
        SubmitAssessmentRequest request = new SubmitAssessmentRequest(testSnapshotId);

        mockMvc.perform(post("/api/v1/assessments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());

        verify(assessmentService, never()).submitAssessment(any(), any());
    }

    // ========== getAssessment() Tests ==========

    @Test
    @WithMockUser(roles = "LEARNER")
    void testGetAssessment_Success() throws Exception {
        when(authenticationUtil.getCurrentUserId()).thenReturn(testUserId);
        when(assessmentService.getAssessment(testAssessmentId, testUserId))
            .thenReturn(testAssessment);

        mockMvc.perform(get("/api/v1/assessments/{id}", testAssessmentId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(testAssessmentId.toString())))
            .andExpect(jsonPath("$.userId", is(testUserId.toString())))
            .andExpect(jsonPath("$.snapshotId", is(testSnapshotId.toString())))
            .andExpect(jsonPath("$.status", is("PENDING")));

        verify(authenticationUtil, times(1)).getCurrentUserId();
        verify(assessmentService, times(1)).getAssessment(testAssessmentId, testUserId);
    }

    @Test
    @WithMockUser(roles = "LEARNER")
    void testGetAssessment_NotFound() throws Exception {
        when(authenticationUtil.getCurrentUserId()).thenReturn(testUserId);
        when(assessmentService.getAssessment(testAssessmentId, testUserId))
            .thenThrow(new AssessmentNotFoundException("Assessment not found or not owned by user"));

        mockMvc.perform(get("/api/v1/assessments/{id}", testAssessmentId))
            .andExpect(status().isNotFound());

        verify(assessmentService, times(1)).getAssessment(testAssessmentId, testUserId);
    }

    @Test
    @WithMockUser(roles = "LEARNER")
    void testGetAssessment_NotOwnedByUser() throws Exception {
        UUID differentUserId = UUID.randomUUID();
        when(authenticationUtil.getCurrentUserId()).thenReturn(differentUserId);
        when(assessmentService.getAssessment(testAssessmentId, differentUserId))
            .thenThrow(new AssessmentNotFoundException("Assessment not found or not owned by user"));

        mockMvc.perform(get("/api/v1/assessments/{id}", testAssessmentId))
            .andExpect(status().isNotFound());

        verify(assessmentService, times(1)).getAssessment(testAssessmentId, differentUserId);
    }

    @Test
    void testGetAssessment_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/assessments/{id}", testAssessmentId))
            .andExpect(status().isUnauthorized());

        verify(assessmentService, never()).getAssessment(any(), any());
    }

    // ========== getUserAssessments() Tests ==========

    @Test
    @WithMockUser(roles = "LEARNER")
    void testGetUserAssessments_Success() throws Exception {
        Assessment assessment2 = Assessment.builder()
            .id(UUID.randomUUID())
            .userId(testUserId)
            .snapshotId(UUID.randomUUID())
            .status(AssessmentStatus.PROCESSING)
            .createdAt(LocalDateTime.now().minusHours(1))
            .build();

        List<Assessment> assessmentList = List.of(testAssessment, assessment2);

        when(authenticationUtil.getCurrentUserId()).thenReturn(testUserId);
        when(assessmentService.getUserAssessments(testUserId))
            .thenReturn(assessmentList);

        mockMvc.perform(get("/api/v1/assessments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assessments", hasSize(2)))
            .andExpect(jsonPath("$.total", is(2)))
            .andExpect(jsonPath("$.assessments[0].status", is("PENDING")))
            .andExpect(jsonPath("$.assessments[1].status", is("PROCESSING")));

        verify(authenticationUtil, times(1)).getCurrentUserId();
        verify(assessmentService, times(1)).getUserAssessments(testUserId);
    }

    @Test
    @WithMockUser(roles = "LEARNER")
    void testGetUserAssessments_EmptyList() throws Exception {
        when(authenticationUtil.getCurrentUserId()).thenReturn(testUserId);
        when(assessmentService.getUserAssessments(testUserId))
            .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/assessments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assessments", hasSize(0)))
            .andExpect(jsonPath("$.total", is(0)));

        verify(assessmentService, times(1)).getUserAssessments(testUserId);
    }

    @Test
    void testGetUserAssessments_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/assessments"))
            .andExpect(status().isUnauthorized());

        verify(assessmentService, never()).getUserAssessments(any());
    }
}
