package com.learnhub.github.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Integer> languages;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String filesContent;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id")
    private List<FileSnapshot> files = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Analysis helper methods
    public String getLanguage() {
        if (languages == null || languages.isEmpty()) {
            return "Unknown";
        }
        return languages.entrySet().stream()
            .max(Comparator.comparingInt(Map.Entry::getValue))
            .map(Map.Entry::getKey)
            .orElse("Unknown");
    }

    public String getFilesContent() {
        return filesContent != null ? filesContent : "";
    }

    public List<FileSnapshot> getTopFiles() {
        return files.stream()
            .sorted(Comparator.comparingInt(FileSnapshot::getLineCount).reversed())
            .limit(10)
            .collect(Collectors.toList());
    }

    public Instant getCreatedAtAsInstant() {
        return createdAt != null ? createdAt.toInstant(java.time.ZoneOffset.UTC) : Instant.now();
    }
}
