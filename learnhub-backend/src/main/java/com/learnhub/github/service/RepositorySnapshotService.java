package com.learnhub.github.service;

import com.learnhub.github.entity.RepositorySnapshot;
import com.learnhub.github.exception.SnapshotNotFoundException;
import com.learnhub.github.repository.RepositorySnapshotRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing repository snapshots.
 * Handles creation and retrieval of point-in-time repository metadata snapshots.
 */
@Service
@Transactional
@Slf4j
public class RepositorySnapshotService {

    private final RepositorySnapshotRepository repository;
    private final RepositorySelectionService selectionService;

    public RepositorySnapshotService(
        RepositorySnapshotRepository repository,
        RepositorySelectionService selectionService
    ) {
        this.repository = repository;
        this.selectionService = selectionService;
    }

    /**
     * Create a snapshot of a GitHub repository.
     *
     * @param userId the user ID
     * @param githubRepoId the GitHub repository ID
     * @param branch the branch name (defaults to "main" if null)
     * @param commitSha the commit SHA
     * @param filesCount the number of files in the repository
     * @param totalSizeKb the total repository size in KB
     * @param languages a map of programming languages to file counts
     * @return the created snapshot
     * @throws IllegalArgumentException if parameters are invalid
     * @throws com.learnhub.github.exception.RepositoryNotSelectedException if repository not selected
     */
    public RepositorySnapshot createSnapshot(
        UUID userId,
        Long githubRepoId,
        String branch,
        String commitSha,
        Integer filesCount,
        Long totalSizeKb,
        Map<String, Integer> languages
    ) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        if (githubRepoId == null || githubRepoId <= 0) {
            throw new IllegalArgumentException("GitHub repository ID must be positive");
        }
        if (filesCount == null || filesCount < 0) {
            throw new IllegalArgumentException("Files count must be non-negative");
        }
        if (totalSizeKb == null || totalSizeKb < 0) {
            throw new IllegalArgumentException("Total size must be non-negative");
        }

        // Verify repository was selected
        selectionService.findSelectedRepository(userId, githubRepoId);

        RepositorySnapshot snapshot = RepositorySnapshot.builder()
            .userId(userId)
            .githubRepoId(githubRepoId)
            .branch(branch != null ? branch : "main")
            .commitSha(commitSha)
            .filesCount(filesCount)
            .totalSizeKb(totalSizeKb)
            .languages(languages)
            .build();

        RepositorySnapshot saved = repository.save(snapshot);
        log.info("Snapshot created for repository {} by user {}", githubRepoId, userId);
        return saved;
    }

    /**
     * Get a snapshot by ID with ownership verification.
     *
     * @param snapshotId the snapshot ID
     * @param userId the user ID for ownership verification
     * @return the snapshot
     * @throws IllegalArgumentException if IDs are null
     * @throws SnapshotNotFoundException if snapshot not found or not owned by user
     */
    @Transactional(readOnly = true)
    public RepositorySnapshot getSnapshot(UUID snapshotId, UUID userId) {
        if (snapshotId == null || userId == null) {
            throw new IllegalArgumentException("Snapshot ID and User ID must not be null");
        }

        return repository.findByIdAndUserId(snapshotId, userId)
            .orElseThrow(() -> new SnapshotNotFoundException(
                "Snapshot not found or not owned by user: " + snapshotId
            ));
    }

    /**
     * Delete a snapshot with ownership verification.
     *
     * @param snapshotId the snapshot ID
     * @param userId the user ID for ownership verification
     * @throws IllegalArgumentException if IDs are null
     * @throws SnapshotNotFoundException if snapshot not found or not owned by user
     */
    public void deleteSnapshot(UUID snapshotId, UUID userId) {
        if (snapshotId == null || userId == null) {
            throw new IllegalArgumentException("Snapshot ID and User ID must not be null");
        }

        RepositorySnapshot snapshot = getSnapshot(snapshotId, userId);
        repository.delete(snapshot);
        log.info("Snapshot {} deleted by user {}", snapshotId, userId);
    }

    /**
     * Find all snapshots for a user, ordered by creation date (newest first).
     *
     * @param userId the user ID
     * @return list of snapshots for the user
     */
    @Transactional(readOnly = true)
    public List<RepositorySnapshot> findUserSnapshots(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
