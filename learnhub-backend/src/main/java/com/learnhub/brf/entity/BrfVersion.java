package com.learnhub.brf.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a version of the BRF (Behavior and Rule Framework).
 *
 * Each version contains complete YAML rules for competency evaluation.
 * Supports semantic versioning and Git-based version control.
 *
 * BRF versions are immutable once created (no updates).
 * Rolling back is achieved by creating new version from old commit.
 */
@Entity
@Table(name = "brf_versions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrfVersion {

    /**
     * Unique identifier for this BRF version.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Semantic version string (e.g., "1.0.0", "1.2.3-beta").
     * Must be unique across all BRF versions.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String version;

    /**
     * Git tag associated with this version (e.g., "v1.0.0").
     * Optional; used to link BRF version to Git history.
     */
    @Column(nullable = true, length = 100)
    private String gitTag;

    /**
     * Git commit hash where this version was defined.
     * Optional; used for version control and rollback.
     */
    @Column(nullable = true, length = 40)
    private String commitHash;

    /**
     * URL to downloadable artifact (e.g., S3, MinIO).
     * Optional; used if YAML is stored externally.
     * Falls back to yamlContent if artifact unavailable.
     */
    @Column(nullable = true, columnDefinition = "TEXT")
    private String artifactUrl;

    /**
     * Complete YAML content for this BRF version.
     * Contains all competency rules and evaluation criteria.
     * Stored in DB as primary source of truth.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String yamlContent;

    /**
     * Timestamp when this version was created.
     */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Timestamp of last update (if any).
     * Immutable after creation, so this rarely changes.
     */
    @Column(nullable = false)
    private Instant updatedAt;

    /**
     * Auto-populate timestamps on creation.
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    /**
     * Update timestamp on modification.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
