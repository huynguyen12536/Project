package com.learnhub.github.service;

import com.learnhub.github.entity.UserGithubSelection;
import com.learnhub.github.exception.RepositoryAlreadySelectedException;
import com.learnhub.github.exception.RepositoryNotSelectedException;
import com.learnhub.github.repository.UserGithubSelectionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing GitHub repository selections.
 * Handles user selections of GitHub repositories for analysis and learning.
 */
@Service
@Transactional
@Slf4j
public class RepositorySelectionService {

    private final UserGithubSelectionRepository repository;

    public RepositorySelectionService(UserGithubSelectionRepository repository) {
        this.repository = repository;
    }

    /**
     * Select a GitHub repository for the user.
     *
     * @param userId the user ID
     * @param githubRepoId the GitHub repository ID
     * @return the created selection
     * @throws IllegalArgumentException if user ID is null or repo ID is invalid
     * @throws RepositoryAlreadySelectedException if repository is already selected
     */
    public UserGithubSelection selectRepository(UUID userId, Long githubRepoId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        if (githubRepoId == null || githubRepoId <= 0) {
            throw new IllegalArgumentException("GitHub repository ID must be positive");
        }

        if (isRepositorySelected(userId, githubRepoId)) {
            throw new RepositoryAlreadySelectedException(
                "Repository " + githubRepoId + " already selected by user"
            );
        }

        UserGithubSelection selection = UserGithubSelection.builder()
            .userId(userId)
            .githubRepoId(githubRepoId)
            .build();

        UserGithubSelection saved = repository.save(selection);
        log.info("Repository {} selected by user {}", githubRepoId, userId);
        return saved;
    }

    /**
     * Check if a repository has been selected by the user.
     *
     * @param userId the user ID
     * @param githubRepoId the GitHub repository ID
     * @return true if selected, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isRepositorySelected(UUID userId, Long githubRepoId) {
        return repository.existsByUserIdAndGithubRepoId(userId, githubRepoId);
    }

    /**
     * Find a user's selection for a specific repository.
     *
     * @param userId the user ID
     * @param githubRepoId the GitHub repository ID
     * @return the selection
     * @throws RepositoryNotSelectedException if repository is not selected
     */
    @Transactional(readOnly = true)
    public UserGithubSelection findSelectedRepository(UUID userId, Long githubRepoId) {
        return repository.findByUserIdAndGithubRepoId(userId, githubRepoId)
            .orElseThrow(() -> new RepositoryNotSelectedException(
                "Repository not selected: " + githubRepoId
            ));
    }

    /**
     * Find all repositories selected by the user.
     *
     * @param userId the user ID
     * @return list of user's selections
     */
    @Transactional(readOnly = true)
    public List<UserGithubSelection> findUserSelections(UUID userId) {
        return repository.findByUserId(userId);
    }
}
