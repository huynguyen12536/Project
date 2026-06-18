package com.learnhub.github.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a user's GitHub repository selection.
 * Tracks which repositories a user has selected for analysis or learning.
 */
@Entity
@Table(
    name = "user_github_selections",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "github_repo_id"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserGithubSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private Long githubRepoId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime selectedAt;
}
