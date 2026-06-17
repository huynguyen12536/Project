package com.learnhub.user.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "account_lockouts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountLockout {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime lockedUntil;

    @Column(nullable = false)
    private Integer failedAttempts = 0;

    private LocalDateTime lastFailedAttempt;

    private String lockReason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime releasedAt;

    @Column(nullable = false)
    private Boolean isActive = true;

    public boolean isCurrentlyLocked() {
        return isActive && LocalDateTime.now().isBefore(lockedUntil);
    }
}
