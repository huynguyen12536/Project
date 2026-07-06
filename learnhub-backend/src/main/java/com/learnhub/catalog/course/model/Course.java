package com.learnhub.catalog.course.model;

import com.learnhub.catalog.taxonomy.model.CourseCategory;
import com.learnhub.catalog.taxonomy.model.CourseLevel;
import com.learnhub.catalog.taxonomy.model.CourseLanguage;
import com.learnhub.catalog.taxonomy.model.CourseSubcategory;
import com.learnhub.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    private User instructor;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 500)
    private String subtitle;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(name = "promo_video_url", columnDefinition = "TEXT")
    private String promoVideoUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CourseCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategory_id")
    private CourseSubcategory subcategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "level_id")
    private CourseLevel level;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id")
    private CourseLanguage language;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<CourseSection> sections = new ArrayList<>();

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private CourseStatus status;

    @Column(name = "total_video_duration_seconds", nullable = false)
    private Integer totalVideoDurationSeconds;

    @Column(name = "lecture_count", nullable = false)
    private Integer lectureCount;

    @Column(name = "student_count", nullable = false)
    private Integer studentCount;

    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum CourseStatus {
        DRAFT, PENDING_REVIEW, PUBLISHED, REJECTED
    }
}
