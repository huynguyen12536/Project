package com.learnhub.upload.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "uploaded_media_objects",
    uniqueConstraints = @UniqueConstraint(name = "ux_uploaded_media_bucket_object", columnNames = {"bucket_name", "object_key"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadedMediaObject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_id", unique = true)
    private UUID sessionId;

    @Column(name = "object_key", nullable = false, columnDefinition = "TEXT")
    private String objectKey;

    @Column(name = "bucket_name", nullable = false, length = 255)
    private String bucketName;

    @Column(name = "public_url", nullable = false, columnDefinition = "TEXT")
    private String publicUrl;

    @Column(name = "filename", nullable = false, length = 512)
    private String filename;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "content_type", nullable = false, length = 255)
    private String contentType;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 50)
    private UploadAssetType assetType;

    @Column(name = "etag", length = 255)
    private String etag;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "course_id")
    private UUID courseId;

    @Column(name = "lecture_id")
    private UUID lectureId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
