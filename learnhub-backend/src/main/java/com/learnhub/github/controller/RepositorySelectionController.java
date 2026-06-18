package com.learnhub.github.controller;

import com.learnhub.github.dto.CreateSnapshotRequest;
import com.learnhub.github.dto.SelectRepositoryRequest;
import com.learnhub.github.dto.SelectRepositoryResponse;
import com.learnhub.github.dto.SnapshotResponse;
import com.learnhub.github.entity.RepositorySnapshot;
import com.learnhub.github.entity.UserGithubSelection;
import com.learnhub.github.service.RepositorySelectionService;
import com.learnhub.github.service.RepositorySnapshotService;
import com.learnhub.user.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

/**
 * Controller for repository selection and snapshot management.
 * Handles endpoints for selecting GitHub repositories and creating/retrieving snapshots.
 */
@RestController
@RequestMapping("/api/v1/repositories")
@PreAuthorize("hasRole('LEARNER')")
@Validated
@Slf4j
public class RepositorySelectionController {

    private final RepositorySelectionService selectionService;
    private final RepositorySnapshotService snapshotService;

    public RepositorySelectionController(
        RepositorySelectionService selectionService,
        RepositorySnapshotService snapshotService
    ) {
        this.selectionService = selectionService;
        this.snapshotService = snapshotService;
    }

    /**
     * Select a GitHub repository for the authenticated user.
     *
     * @param repoId the GitHub repository ID
     * @param request the selection request
     * @param authentication the authenticated user
     * @return 201 Created with the selection details
     */
    @PostMapping("/{repoId}/select")
    public ResponseEntity<SelectRepositoryResponse> selectRepository(
        @PathVariable @Positive(message = "Repository ID must be positive") Long repoId,
        @Valid @RequestBody SelectRepositoryRequest request,
        Authentication authentication
    ) {
        log.info("Selecting repository {} for user {}", repoId, authentication.getName());

        User user = (User) authentication.getDetails();
        UUID userId = user.getId();

        UserGithubSelection selection = selectionService.selectRepository(userId, repoId);

        SelectRepositoryResponse response = SelectRepositoryResponse.builder()
            .selectionId(selection.getId())
            .githubRepoId(selection.getGithubRepoId())
            .selectedAt(selection.getSelectedAt())
            .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Create a snapshot of a GitHub repository for the authenticated user.
     *
     * @param repoId the GitHub repository ID
     * @param request the snapshot creation request
     * @param authentication the authenticated user
     * @return 201 Created with the snapshot details
     */
    @PostMapping("/{repoId}/snapshot")
    public ResponseEntity<SnapshotResponse> createSnapshot(
        @PathVariable @Positive(message = "Repository ID must be positive") Long repoId,
        @Valid @RequestBody CreateSnapshotRequest request,
        Authentication authentication
    ) {
        log.info("Creating snapshot for repository {} by user {}", repoId, authentication.getName());

        User user = (User) authentication.getDetails();
        UUID userId = user.getId();

        RepositorySnapshot snapshot = snapshotService.createSnapshot(
            userId,
            repoId,
            request.getBranch(),
            request.getCommitSha(),
            request.getFilesCount(),
            request.getTotalSizeKb(),
            request.getLanguages()
        );

        SnapshotResponse response = SnapshotResponse.builder()
            .snapshotId(snapshot.getId())
            .githubRepoId(snapshot.getGithubRepoId())
            .branch(snapshot.getBranch())
            .commitSha(snapshot.getCommitSha())
            .filesCount(snapshot.getFilesCount())
            .totalSizeKb(snapshot.getTotalSizeKb())
            .languages(snapshot.getLanguages())
            .createdAt(snapshot.getCreatedAt())
            .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieve a snapshot by ID for the authenticated user.
     *
     * @param snapshotId the snapshot ID
     * @param authentication the authenticated user
     * @return 200 OK with the snapshot details
     */
    @GetMapping("/snapshots/{snapshotId}")
    public ResponseEntity<SnapshotResponse> getSnapshot(
        @PathVariable UUID snapshotId,
        Authentication authentication
    ) {
        log.info("Retrieving snapshot {} for user {}", snapshotId, authentication.getName());

        User user = (User) authentication.getDetails();
        UUID userId = user.getId();

        RepositorySnapshot snapshot = snapshotService.getSnapshot(snapshotId, userId);

        SnapshotResponse response = SnapshotResponse.builder()
            .snapshotId(snapshot.getId())
            .githubRepoId(snapshot.getGithubRepoId())
            .branch(snapshot.getBranch())
            .commitSha(snapshot.getCommitSha())
            .filesCount(snapshot.getFilesCount())
            .totalSizeKb(snapshot.getTotalSizeKb())
            .languages(snapshot.getLanguages())
            .createdAt(snapshot.getCreatedAt())
            .build();

        return ResponseEntity.ok(response);
    }
}
