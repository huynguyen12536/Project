package com.learnhub.upload.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

@Component
@RequiredArgsConstructor
@Slf4j
public class ObjectStorageBootstrap {

    private final S3Client s3Client;
    private final StorageProperties storageProperties;

    @PostConstruct
    public void initializeBucket() {
        try {
            log.info("Initializing object storage bucket '{}'", storageProperties.getBucketName());
            ensureBucketExists();
            log.info("Bucket policy and CORS are managed by the MinIO init job");
            log.info("Object storage bootstrap completed for bucket '{}'", storageProperties.getBucketName());
        } catch (Exception exception) {
            log.error(
                "Object storage bootstrap failed for bucket '{}'. Backend startup will continue without blocking uploads initialization.",
                storageProperties.getBucketName(),
                exception
            );
        }
    }

    private void ensureBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(storageProperties.getBucketName()).build());
            log.info("Object storage bucket '{}' is ready", storageProperties.getBucketName());
        } catch (SdkException exception) {
            s3Client.createBucket(
                CreateBucketRequest.builder()
                    .bucket(storageProperties.getBucketName())
                    .build()
            );
            log.info("Created object storage bucket '{}'", storageProperties.getBucketName());
        }
    }
}
