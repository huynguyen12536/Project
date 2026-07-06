package com.learnhub.upload.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "multipart_upload_sessions",
    uniqueConstraints = @UniqueConstraint(name = "ux_multipart_upload_identity", columnNames = {"upload_id", "object_key"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MultipartUploadSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "upload_id", nullable = false, length = 255)
    private String uploadId;

    @Column(name = "object_key", nullable = false, columnDefinition = "TEXT")
    private String objectKey;

    @Column(name = "bucket_name", nullable = false, length = 255)
    private String bucketName;

    @Column(name = "original_filename", nullable = false, length = 512)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 255)
    private String contentType;

    @Column(name = "total_size", nullable = false)
    private Long totalSize;

    @Column(name = "chunk_size_bytes", nullable = false)
    private Long chunkSizeBytes;

    @Column(name = "max_concurrency", nullable = false)
    private Integer maxConcurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 50)
    private UploadAssetType assetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private MultipartUploadStatus status;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "course_id")
    private UUID courseId;

    @Column(name = "lecture_id")
    private UUID lectureId;

    @Column(name = "public_url", columnDefinition = "TEXT")
    private String publicUrl;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "aborted_at")
    private LocalDateTime abortedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
