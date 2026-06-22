package com.learnhub.brf.repository;

import com.learnhub.brf.entity.BrfVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for BRF version entity operations.
 * Provides queries for loading BRF rules by version.
 */
@Repository
public interface BrfVersionRepository extends JpaRepository<BrfVersion, UUID> {

    /**
     * Find a BRF version by semantic version string.
     *
     * @param version semantic version (e.g., "1.0.0")
     * @return Optional containing BrfVersion if found
     */
    Optional<BrfVersion> findByVersion(String version);

    /**
     * Find a BRF version by Git tag.
     *
     * @param gitTag the Git tag (e.g., "v1.0.0")
     * @return Optional containing BrfVersion if found
     */
    Optional<BrfVersion> findByGitTag(String gitTag);

    /**
     * Check if a version exists.
     *
     * @param version semantic version string
     * @return true if version exists, false otherwise
     */
    boolean existsByVersion(String version);
}
