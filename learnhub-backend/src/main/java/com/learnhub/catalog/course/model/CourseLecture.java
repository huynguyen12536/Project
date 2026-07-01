package com.learnhub.catalog.course.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "course_lectures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseLecture {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private CourseSection section;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private LectureType type;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "video_url", columnDefinition = "TEXT")
    private String videoUrl;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_free_preview", nullable = false)
    private Boolean isFreePreview;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum LectureType {
        VIDEO, ARTICLE, QUIZ
    }
}
