package com.learnhub.assessment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing the results of a completed assessment.
 * Linked to Assessment via one-to-one relationship.
 *
 * Stores the detailed evaluation output from the scoring engine.
 */
@Entity
@Table(name = "assessment_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
@ToString
public class AssessmentResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID assessmentId;

    @Column(nullable = false)
    private String overallLevel;  // NOT_DEMONSTRATED, EMERGING, PROFICIENT, ADVANCED

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> allGaps;  // All gaps identified across competencies

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> nextSteps;  // Recommended next actions for learner

    @Column(nullable = false)
    private Double overallConfidence;  // 0.0 - 1.0

    @Column(columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private String resultsJson;  // Full assessment result blob from scoring engine

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
