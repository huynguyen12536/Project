package com.learnhub.github.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.types.JsonType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing a point-in-time snapshot of a GitHub repository's metadata.
 * Captures repository state at a specific commit and branch for historical analysis.
 */
@Entity
@Table(name = "repository_snapshots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepositorySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private Long githubRepoId;

    @Column(nullable = false)
    private String branch;

    @Column(columnDefinition = "VARCHAR(40)")
    private String commitSha;

    @Column(nullable = false)
    private Integer filesCount;

    @Column(nullable = false)
    private Long totalSizeKb;

    @Column(columnDefinition = "JSONB")
    @Type(JsonType.class)
    private Map<String, Integer> languages;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
