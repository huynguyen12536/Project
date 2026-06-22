package com.learnhub.user.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
@ToString
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String role = "LEARNER";

    // Profile Information (Enhanced for Profile Management)
    @Column(length = 50)
    private String firstName;

    @Column(length = 50)
    private String lastName;

    @Column(length = 500)
    private String bio;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    // Legacy fields (kept for backward compatibility)
    private String phone;
    private String location;
    private String githubProfileUrl;

    // Email Verification
    @Column(nullable = false)
    private Boolean emailVerified = false;
    private LocalDateTime emailVerifiedAt;

    // Security Tracking
    private LocalDateTime lastLogin;
    private LocalDateTime lastPasswordChange;
    private Boolean passwordChangeRequired = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public boolean isEmailVerified() {
        return emailVerified != null && emailVerified;
    }

    public String getFullName() {
        if (firstName == null || lastName == null) {
            return email;
        }
        return firstName + " " + lastName;
    }
}
