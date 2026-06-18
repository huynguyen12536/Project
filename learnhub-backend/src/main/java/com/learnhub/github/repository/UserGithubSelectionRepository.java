package com.learnhub.github.repository;

import com.learnhub.github.entity.UserGithubSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing UserGithubSelection entities.
 * Handles database operations for user GitHub repository selections.
 */
@Repository
public interface UserGithubSelectionRepository extends JpaRepository<UserGithubSelection, UUID> {

    /**
     * Find a user's selection for a specific GitHub repository.
     *
     * @param userId the user ID
     * @param githubRepoId the GitHub repository ID
     * @return Optional containing the selection if found
     */
    Optional<UserGithubSelection> findByUserIdAndGithubRepoId(UUID userId, Long githubRepoId);

    /**
     * Find all repositories selected by a user.
     *
     * @param userId the user ID
     * @return List of selections for the user
     */
    List<UserGithubSelection> findByUserId(UUID userId);

    /**
     * Check if a repository has been selected by a user.
     *
     * @param userId the user ID
     * @param githubRepoId the GitHub repository ID
     * @return true if the repository is selected, false otherwise
     */
    boolean existsByUserIdAndGithubRepoId(UUID userId, Long githubRepoId);
}
