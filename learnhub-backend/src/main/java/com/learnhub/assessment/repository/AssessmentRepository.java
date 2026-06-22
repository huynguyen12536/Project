package com.learnhub.assessment.repository;

import com.learnhub.assessment.entity.Assessment;
import com.learnhub.assessment.entity.AssessmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Assessment entity operations.
 * Provides custom queries for assessments filtering and retrieval.
 */
@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, UUID> {

    /**
     * Find an assessment by its ID and user ID (ownership check).
     */
    Optional<Assessment> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Find all assessments for a user, ordered by creation date descending.
     */
    List<Assessment> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find all assessments with a specific status.
     */
    List<Assessment> findByStatus(AssessmentStatus status);

    /**
     * Find all assessments for a specific snapshot.
     */
    List<Assessment> findBySnapshotId(UUID snapshotId);

    /**
     * Count active (PROCESSING) assessments for a user.
     * Used for rate limiting: max 3 concurrent PROCESSING assessments per user.
     *
     * @param userId the user ID
     * @return count of active assessments
     */
    long countByUserIdAndStatus(UUID userId, AssessmentStatus status);

    /**
     * Find recent assessment for same user and snapshot within time window.
     * Used for duplicate submission detection: prevent resubmission within 60 seconds.
     *
     * @param userId the user ID
     * @param snapshotId the snapshot ID
     * @param afterTime the cutoff time (Instant.now().minusSeconds(60))
     * @return Optional containing recent assessment if found
     */
    @Query("SELECT a FROM Assessment a WHERE a.userId = :userId " +
           "AND a.snapshotId = :snapshotId AND a.createdAt > :afterTime " +
           "ORDER BY a.createdAt DESC LIMIT 1")
    Optional<Assessment> findRecentByUserAndSnapshot(
        @Param("userId") UUID userId,
        @Param("snapshotId") UUID snapshotId,
        @Param("afterTime") Instant afterTime
    );
}
