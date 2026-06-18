package com.learnhub.github.repository;

import com.learnhub.github.entity.RepositorySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing RepositorySnapshot entities.
 * Handles database operations for repository metadata snapshots.
 */
@Repository
public interface RepositorySnapshotRepository extends JpaRepository<RepositorySnapshot, UUID> {

    /**
     * Find a snapshot by ID and verify ownership by user.
     *
     * @param id the snapshot ID
     * @param userId the user ID for ownership verification
     * @return Optional containing the snapshot if found and owned by the user
     */
    Optional<RepositorySnapshot> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Find all snapshots for a user, ordered by creation date (newest first).
     *
     * @param userId the user ID
     * @return List of snapshots ordered by creation date descending
     */
    List<RepositorySnapshot> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find all snapshots for a repository, ordered by creation date (newest first).
     *
     * @param githubRepoId the GitHub repository ID
     * @return List of snapshots ordered by creation date descending
     */
    List<RepositorySnapshot> findByGithubRepoIdOrderByCreatedAtDesc(Long githubRepoId);
}
