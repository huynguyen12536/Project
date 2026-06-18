package com.learnhub.github.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnhub.github.dto.CreateSnapshotRequest;
import com.learnhub.github.dto.SelectRepositoryRequest;
import com.learnhub.github.entity.RepositorySnapshot;
import com.learnhub.github.entity.UserGithubSelection;
import com.learnhub.github.exception.RepositoryAlreadySelectedException;
import com.learnhub.github.exception.RepositoryNotSelectedException;
import com.learnhub.github.exception.SnapshotNotFoundException;
import com.learnhub.github.service.RepositorySelectionService;
import com.learnhub.github.service.RepositorySnapshotService;
import com.learnhub.user.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for RepositorySelectionController.
 * Tests all endpoints with various scenarios including success and error cases.
 */
@WebMvcTest(RepositorySelectionController.class)
class RepositorySelectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RepositorySelectionService selectionService;

    @MockBean
    private RepositorySnapshotService snapshotService;

    private static final UUID TEST_USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID TEST_SNAPSHOT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final Long TEST_REPO_ID = 12345L;
    private static final String TEST_EMAIL = "test@example.com";

    private User createTestUser() {
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);
        user.setRole("LEARNER");
        return user;
    }

    private UsernamePasswordAuthenticationToken createAuthentication(User user) {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
            user.getEmail(),
            null,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_LEARNER"))
        );
        token.setDetails(user);
        return token;
    }

    // ============ SELECT REPOSITORY ENDPOINT TESTS ============

    @Test
    void testSelectRepository_WithValidInput_Returns201Created() throws Exception {
        // Arrange
        User user = createTestUser();
        SelectRepositoryRequest request = new SelectRepositoryRequest(TEST_REPO_ID);
        LocalDateTime now = LocalDateTime.now();
        UserGithubSelection selection = UserGithubSelection.builder()
            .id(UUID.randomUUID())
            .userId(TEST_USER_ID)
            .githubRepoId(TEST_REPO_ID)
            .selectedAt(now)
            .build();

        when(selectionService.selectRepository(TEST_USER_ID, TEST_REPO_ID))
            .thenReturn(selection);

        // Act & Assert
        mockMvc.perform(post("/api/v1/repositories/{repoId}/select", TEST_REPO_ID)
                .with(authentication(createAuthentication(user)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.githubRepoId", is(TEST_REPO_ID.intValue())))
            .andExpect(jsonPath("$.selectionId").exists());
    }

    @Test
    void testSelectRepository_WithoutAuthentication_Returns401Unauthorized() throws Exception {
        // Arrange
        SelectRepositoryRequest request = new SelectRepositoryRequest(TEST_REPO_ID);

        // Act & Assert
        mockMvc.perform(post("/api/v1/repositories/{repoId}/select", TEST_REPO_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void testSelectRepository_WithNullRepoId_Returns400BadRequest() throws Exception {
        // Arrange
        User user = createTestUser();
        SelectRepositoryRequest request = new SelectRepositoryRequest(null);

        // Act & Assert
        mockMvc.perform(post("/api/v1/repositories/{repoId}/select", TEST_REPO_ID)
                .with(authentication(createAuthentication(user)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    void testSelectRepository_WithNegativeRepoId_Returns400BadRequest() throws Exception {
        // Arrange
        User user = createTestUser();

        // Act & Assert
        mockMvc.perform(post("/api/v1/repositories/{repoId}/select", -1)
                .with(authentication(createAuthentication(user)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testSelectRepository_WhenRepositoryAlreadySelected_Returns409Conflict() throws Exception {
        // Arrange
        User user = createTestUser();
        SelectRepositoryRequest request = new SelectRepositoryRequest(TEST_REPO_ID);

        when(selectionService.selectRepository(TEST_USER_ID, TEST_REPO_ID))
            .thenThrow(new RepositoryAlreadySelectedException("Repository " + TEST_REPO_ID + " already selected by user"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/repositories/{repoId}/select", TEST_REPO_ID)
                .with(authentication(createAuthentication(user)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error", is("REPOSITORY_ALREADY_SELECTED")));
    }

    // ============ CREATE SNAPSHOT ENDPOINT TESTS ============

    @Test
    void testCreateSnapshot_WithValidInput_Returns201Created() throws Exception {
        // Arrange
        User user = createTestUser();
        Map<String, Integer> languages = new HashMap<>();
        languages.put("Java", 45);
        languages.put("Python", 30);

        CreateSnapshotRequest request = new CreateSnapshotRequest(
            TEST_REPO_ID,
            "main",
            "abc123def456abc123def456abc123def456abc1",
            150,
            2048L,
            languages
        );

        LocalDateTime now = LocalDateTime.now();
        RepositorySnapshot snapshot = RepositorySnapshot.builder()
            .id(TEST_SNAPSHOT_ID)
            .userId(TEST_USER_ID)
            .githubRepoId(TEST_REPO_ID)
            .branch("main")
            .commitSha("abc123def456abc123def456abc123def456abc1")
            .filesCount(150)
            .totalSizeKb(2048L)
            .languages(languages)
            .createdAt(now)
            .build();

        when(snapshotService.createSnapshot(
            eq(TEST_USER_ID),
            eq(TEST_REPO_ID),
            eq("main"),
            eq("abc123def456abc123def456abc123def456abc1"),
            eq(150),
            eq(2048L),
            eq(languages)
        )).thenReturn(snapshot);

        // Act & Assert
        mockMvc.perform(post("/api/v1/repositories/{repoId}/snapshot", TEST_REPO_ID)
                .with(authentication(createAuthentication(user)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.githubRepoId", is(TEST_REPO_ID.intValue())))
            .andExpect(jsonPath("$.snapshotId").exists())
            .andExpect(jsonPath("$.branch", is("main")))
            .andExpect(jsonPath("$.filesCount", is(150)));
    }

    @Test
    void testCreateSnapshot_WithoutAuthentication_Returns401Unauthorized() throws Exception {
        // Arrange
        CreateSnapshotRequest request = new CreateSnapshotRequest(
            TEST_REPO_ID,
            "main",
            "abc123",
            100,
            1024L,
            null
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/repositories/{repoId}/snapshot", TEST_REPO_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void testCreateSnapshot_WithNullFilesCount_Returns400BadRequest() throws Exception {
        // Arrange
        User user = createTestUser();
        CreateSnapshotRequest request = new CreateSnapshotRequest(
            TEST_REPO_ID,
            "main",
            "abc123",
            null,
            1024L,
            null
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/repositories/{repoId}/snapshot", TEST_REPO_ID)
                .with(authentication(createAuthentication(user)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    void testCreateSnapshot_WithNegativeFilesCount_Returns400BadRequest() throws Exception {
        // Arrange
        User user = createTestUser();
        CreateSnapshotRequest request = new CreateSnapshotRequest(
            TEST_REPO_ID,
            "main",
            "abc123",
            -5,
            1024L,
            null
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/repositories/{repoId}/snapshot", TEST_REPO_ID)
                .with(authentication(createAuthentication(user)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    void testCreateSnapshot_WithNullTotalSize_Returns400BadRequest() throws Exception {
        // Arrange
        User user = createTestUser();
        CreateSnapshotRequest request = new CreateSnapshotRequest(
            TEST_REPO_ID,
            "main",
            "abc123",
            100,
            null,
            null
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/repositories/{repoId}/snapshot", TEST_REPO_ID)
                .with(authentication(createAuthentication(user)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    void testCreateSnapshot_WithRepositoryNotSelected_Returns400BadRequest() throws Exception {
        // Arrange
        User user = createTestUser();
        CreateSnapshotRequest request = new CreateSnapshotRequest(
            TEST_REPO_ID,
            "main",
            "abc123",
            100,
            1024L,
            null
        );

        when(snapshotService.createSnapshot(
            any(UUID.class),
            anyLong(),
            any(),
            any(),
            any(),
            any(),
            any()
        )).thenThrow(new RepositoryNotSelectedException("Repository not selected: " + TEST_REPO_ID));

        // Act & Assert
        mockMvc.perform(post("/api/v1/repositories/{repoId}/snapshot", TEST_REPO_ID)
                .with(authentication(createAuthentication(user)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error", is("REPOSITORY_NOT_SELECTED")));
    }

    @Test
    void testCreateSnapshot_WithOversizedBranchName_Returns400BadRequest() throws Exception {
        // Arrange
        User user = createTestUser();
        String longBranch = "a".repeat(256); // Exceeds 255 limit
        CreateSnapshotRequest request = new CreateSnapshotRequest(
            TEST_REPO_ID,
            longBranch,
            "abc123",
            100,
            1024L,
            null
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/repositories/{repoId}/snapshot", TEST_REPO_ID)
                .with(authentication(createAuthentication(user)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    @Test
    void testCreateSnapshot_WithOversizedCommitSha_Returns400BadRequest() throws Exception {
        // Arrange
        User user = createTestUser();
        String longSha = "a".repeat(41); // Exceeds 40 limit
        CreateSnapshotRequest request = new CreateSnapshotRequest(
            TEST_REPO_ID,
            "main",
            longSha,
            100,
            1024L,
            null
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/repositories/{repoId}/snapshot", TEST_REPO_ID)
                .with(authentication(createAuthentication(user)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));
    }

    // ============ GET SNAPSHOT ENDPOINT TESTS ============

    @Test
    void testGetSnapshot_WithValidSnapshotId_Returns200OK() throws Exception {
        // Arrange
        User user = createTestUser();
        LocalDateTime now = LocalDateTime.now();
        Map<String, Integer> languages = new HashMap<>();
        languages.put("Java", 100);

        RepositorySnapshot snapshot = RepositorySnapshot.builder()
            .id(TEST_SNAPSHOT_ID)
            .userId(TEST_USER_ID)
            .githubRepoId(TEST_REPO_ID)
            .branch("main")
            .commitSha("abc123def456abc123def456abc123def456abc1")
            .filesCount(150)
            .totalSizeKb(2048L)
            .languages(languages)
            .createdAt(now)
            .build();

        when(snapshotService.getSnapshot(TEST_SNAPSHOT_ID, TEST_USER_ID))
            .thenReturn(snapshot);

        // Act & Assert
        mockMvc.perform(get("/api/v1/repositories/snapshots/{snapshotId}", TEST_SNAPSHOT_ID)
                .with(authentication(createAuthentication(user)))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.snapshotId").exists())
            .andExpect(jsonPath("$.githubRepoId", is(TEST_REPO_ID.intValue())))
            .andExpect(jsonPath("$.branch", is("main")))
            .andExpect(jsonPath("$.filesCount", is(150)));
    }

    @Test
    void testGetSnapshot_WithoutAuthentication_Returns401Unauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/repositories/snapshots/{snapshotId}", TEST_SNAPSHOT_ID)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetSnapshot_WithNonExistentSnapshot_Returns404NotFound() throws Exception {
        // Arrange
        User user = createTestUser();

        when(snapshotService.getSnapshot(TEST_SNAPSHOT_ID, TEST_USER_ID))
            .thenThrow(new SnapshotNotFoundException("Snapshot not found or not owned by user: " + TEST_SNAPSHOT_ID));

        // Act & Assert
        mockMvc.perform(get("/api/v1/repositories/snapshots/{snapshotId}", TEST_SNAPSHOT_ID)
                .with(authentication(createAuthentication(user)))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error", is("SNAPSHOT_NOT_FOUND")));
    }

    @Test
    void testGetSnapshot_WithSnapshotOwnedByOtherUser_Returns404NotFound() throws Exception {
        // Arrange
        User user = createTestUser();

        when(snapshotService.getSnapshot(TEST_SNAPSHOT_ID, TEST_USER_ID))
            .thenThrow(new SnapshotNotFoundException("Snapshot not found or not owned by user: " + TEST_SNAPSHOT_ID));

        // Act & Assert
        mockMvc.perform(get("/api/v1/repositories/snapshots/{snapshotId}", TEST_SNAPSHOT_ID)
                .with(authentication(createAuthentication(user)))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error", is("SNAPSHOT_NOT_FOUND")));
    }

    @Test
    void testCreateSnapshot_WithMinimalValidInput_Returns201Created() throws Exception {
        // Arrange - test with minimal required fields
        User user = createTestUser();
        CreateSnapshotRequest request = new CreateSnapshotRequest(
            TEST_REPO_ID,
            null,
            null,
            0,
            0L,
            null
        );

        LocalDateTime now = LocalDateTime.now();
        RepositorySnapshot snapshot = RepositorySnapshot.builder()
            .id(TEST_SNAPSHOT_ID)
            .userId(TEST_USER_ID)
            .githubRepoId(TEST_REPO_ID)
            .branch("main")
            .commitSha(null)
            .filesCount(0)
            .totalSizeKb(0L)
            .languages(null)
            .createdAt(now)
            .build();

        when(snapshotService.createSnapshot(
            eq(TEST_USER_ID),
            eq(TEST_REPO_ID),
            any(),
            any(),
            eq(0),
            eq(0L),
            any()
        )).thenReturn(snapshot);

        // Act & Assert
        mockMvc.perform(post("/api/v1/repositories/{repoId}/snapshot", TEST_REPO_ID)
                .with(authentication(createAuthentication(user)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.snapshotId").exists());
    }
}
