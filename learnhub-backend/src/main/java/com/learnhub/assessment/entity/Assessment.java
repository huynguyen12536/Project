package com.learnhub.assessment.entity;

import com.learnhub.assessment.scoring.RadarSeriesDto;
import com.learnhub.github.entity.RepositorySnapshot;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
public class Assessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(name = "snapshot_id", nullable = false)
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

    // Explicit getters (workaround for Lombok annotation processing in Docker)
    public UUID getId() { return this.id; }
    public UUID getUserId() { return this.userId; }
    public UUID getSnapshotId() { return this.snapshotId; }
    public RepositorySnapshot getSnapshot() { return this.snapshot; }
    public AssessmentStatus getStatus() { return this.status; }
    public Instant getCreatedAt() { return this.createdAt; }
    public Instant getStartedAt() { return this.startedAt; }
    public Instant getCompletedAt() { return this.completedAt; }
    public String getErrorMessage() { return this.errorMessage; }
    public RadarSeriesDto getResultsData() { return this.resultsData; }

    // Explicit setters
    public void setId(UUID id) { this.id = id; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public void setSnapshotId(UUID snapshotId) { this.snapshotId = snapshotId; }
    public void setSnapshot(RepositorySnapshot snapshot) { this.snapshot = snapshot; }
    public void setStatus(AssessmentStatus status) { this.status = status; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setResultsData(RadarSeriesDto resultsData) { this.resultsData = resultsData; }
}
