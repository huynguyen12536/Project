package com.learnhub.upload.repository;

import com.learnhub.upload.model.UploadedMediaObject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UploadedMediaObjectRepository extends JpaRepository<UploadedMediaObject, UUID> {
    Optional<UploadedMediaObject> findBySessionId(UUID sessionId);
    Optional<UploadedMediaObject> findByBucketNameAndObjectKey(String bucketName, String objectKey);
}
