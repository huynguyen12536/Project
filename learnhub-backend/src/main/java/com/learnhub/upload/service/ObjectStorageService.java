package com.learnhub.upload.service;

import com.learnhub.upload.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ObjectStorageService {

    private final S3Client s3Client;
    private final StorageProperties storageProperties;

    public HeadObjectResponse headObject(String bucketName, String objectKey) {
        return s3Client.headObject(
            HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build()
        );
    }

    public boolean objectExists(String bucketName, String objectKey) {
        try {
            headObject(bucketName, objectKey);
            return true;
        } catch (NoSuchKeyException exception) {
            return false;
        } catch (Exception exception) {
            return false;
        }
    }

    public ResponseInputStream<GetObjectResponse> getObject(String bucketName, String objectKey) {
        return s3Client.getObject(
            GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build()
        );
    }

    public String putObject(String bucketName, String objectKey, byte[] bytes, String contentType) {
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(contentType)
                .build(),
            RequestBody.fromBytes(bytes)
        );
        return buildPublicUrl(bucketName, objectKey);
    }

    public void deleteObject(String bucketName, String objectKey) {
        s3Client.deleteObject(
            DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build()
        );
    }

    public String buildGeneratedObjectKey(String prefix, UUID userId, String extension) {
        String safeExtension = extension == null || extension.isBlank() ? "bin" : extension;
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        return "%s/%s/%d/%02d/%s.%s".formatted(
            prefix,
            userId,
            now.getYear(),
            now.getMonthValue(),
            UUID.randomUUID(),
            safeExtension
        );
    }

    public String buildPublicUrl(String bucketName, String objectKey) {
        return stripTrailingSlash(storageProperties.getPublicEndpoint()) + "/" + bucketName + "/" + objectKey;
    }

    private String stripTrailingSlash(String value) {
        return value != null && value.endsWith("/")
            ? value.substring(0, value.length() - 1)
            : value;
    }
}
