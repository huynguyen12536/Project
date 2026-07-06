package com.learnhub.upload.repository;

import com.learnhub.upload.model.MultipartUploadSession;
import com.learnhub.upload.model.MultipartUploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;

public interface MultipartUploadSessionRepository extends JpaRepository<MultipartUploadSession, UUID> {
    Optional<MultipartUploadSession> findByUploadIdAndObjectKey(String uploadId, String objectKey);
    Optional<MultipartUploadSession> findByUploadIdAndObjectKeyAndUploadedBy(String uploadId, String objectKey, UUID uploadedBy);
    List<MultipartUploadSession> findByUploadedByAndStatusIn(UUID uploadedBy, List<MultipartUploadStatus> statuses);
    List<MultipartUploadSession> findByLectureIdAndStatusIn(UUID lectureId, List<MultipartUploadStatus> statuses);
    List<MultipartUploadSession> findByStatusInAndExpiresAtBefore(List<MultipartUploadStatus> statuses, LocalDateTime expiresAt);
}
