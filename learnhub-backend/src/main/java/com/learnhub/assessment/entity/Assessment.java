package com.learnhub.assessment.entity;

import com.learnhub.assessment.scoring.RadarSeriesDto;
import com.learnhub.github.entity.RepositorySnapshot;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a code assessment submission.
 * An assessment is created when a user submits a repository snapshot for analysis.
 *
 * Status Flow: PENDING → PROCESSING → COMPLETED/FAILED
 */
@Entity
@Table(name = "assessments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
@ToString
public class Assessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID snapshotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", insertable = false, updatable = false)
    private RepositorySnapshot snapshot;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AssessmentStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = true)
    private Instant startedAt;

    @Column(nullable = true)
    private Instant completedAt;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = true, columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private RadarSeriesDto resultsData;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = AssessmentStatus.PENDING;
        }
    }
}
