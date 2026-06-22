package com.learnhub.assessment.repository;

import com.learnhub.assessment.entity.AssessmentResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for AssessmentResult entity operations.
 * Provides access to assessment evaluation results.
 */
@Repository
public interface AssessmentResultRepository extends JpaRepository<AssessmentResult, UUID> {

    /**
     * Find assessment result by assessment ID.
     * One-to-one relationship: each assessment has at most one result.
     */
    Optional<AssessmentResult> findByAssessmentId(UUID assessmentId);
}
