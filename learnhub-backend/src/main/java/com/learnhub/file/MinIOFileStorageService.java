package com.learnhub.file;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * MinIO (S3-compatible) file storage service implementation.
 */
@Service
@Slf4j
public class MinIOFileStorageService implements FileStorageService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_MIME_TYPES = new HashSet<>(
        Arrays.asList("image/jpeg", "image/png", "image/webp")
    );
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(
        Arrays.asList("jpg", "jpeg", "png", "webp")
    );

    private final MinioClient minioClient;
    private final String bucketName;
    private final String minioEndpoint;
    private final String minioPublicEndpoint;

    public MinIOFileStorageService(
        @Value("${minio.endpoint:http://minio:9000}") String minioEndpoint,
        @Value("${minio.public-endpoint:http://localhost:9000}") String minioPublicEndpoint,
        @Value("${minio.bucket-name:avatars}") String bucketName,
        @Value("${minio.access-key:minioadmin}") String accessKey,
        @Value("${minio.secret-key:minioadmin}") String secretKey
    ) {
        this.minioEndpoint = stripTrailingSlash(minioEndpoint);
        this.minioPublicEndpoint = stripTrailingSlash(minioPublicEndpoint);
        this.bucketName = bucketName;
        this.minioClient = MinioClient.builder()
            .endpoint(this.minioEndpoint)
            .credentials(accessKey, secretKey)
            .build();
    }

    @PostConstruct
    public void ensureBucketReady() {
        try {
            boolean bucketExists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build()
            );

            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Created MinIO bucket '{}'", bucketName);
            }

            String publicReadPolicy = """
                {
                  "Version":"2012-10-17",
                  "Statement":[
                    {
                      "Effect":"Allow",
                      "Principal":{"AWS":["*"]},
                      "Action":["s3:GetObject"],
                      "Resource":["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(bucketName);

            minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder()
                    .bucket(bucketName)
                    .config(publicReadPolicy)
                    .build()
            );
            log.info("MinIO bucket '{}' is ready", bucketName);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to initialize MinIO bucket '" + bucketName + "'", exception);
        }
    }

    @Override
    public String uploadFile(MultipartFile file, UUID userId) throws IOException {
        log.debug("Uploading avatar for user: {}", userId);
        validateFile(file);

        String extension = getFileExtension(file.getOriginalFilename());
        String objectKey = extension.isBlank()
            ? String.format("avatars/%s/avatar", userId)
            : String.format("avatars/%s/avatar.%s", userId, extension.toLowerCase());

        try {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );
        } catch (Exception exception) {
            throw new IOException("Failed to upload file to MinIO", exception);
        }

        String publicUrl = buildPublicUrl(objectKey);
        log.info("Avatar uploaded successfully for user {}: {}", userId, publicUrl);
        return publicUrl;
    }

    @Override
    public void deleteFile(String fileUrl) throws IOException {
        String objectKey = extractObjectKey(fileUrl);
        if (objectKey == null) {
            return;
        }

        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build()
            );
            log.info("Deleted MinIO object: {}", objectKey);
        } catch (Exception exception) {
            throw new IOException("Failed to delete file from MinIO", exception);
        }
    }

    @Override
    public boolean fileExists(String fileUrl) {
        String objectKey = extractObjectKey(fileUrl);
        if (objectKey == null) {
            return false;
        }

        try {
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build()
            );
            return true;
        } catch (ErrorResponseException exception) {
            return false;
        } catch (Exception exception) {
            log.warn("Failed to check MinIO object existence '{}': {}", objectKey, exception.getMessage());
            return false;
        }
    }

    @Override
    public void validateFile(MultipartFile file) throws IllegalArgumentException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                String.format("File size exceeds maximum of %d bytes", MAX_FILE_SIZE)
            );
        }

        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new IllegalArgumentException(
                String.format("File type '%s' is not allowed. Allowed types: %s", mimeType, ALLOWED_MIME_TYPES)
            );
        }

        String extension = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException(
                String.format("File extension '.%s' is not allowed. Allowed extensions: %s", extension, ALLOWED_EXTENSIONS)
            );
        }
    }

    private String buildPublicUrl(String objectKey) {
        return String.format("%s/%s/%s", minioPublicEndpoint, bucketName, objectKey);
    }

    private String extractObjectKey(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return null;
        }

        try {
            String path = URI.create(fileUrl).getPath();
            String bucketPrefix = "/" + bucketName + "/";
            int bucketIndex = path.indexOf(bucketPrefix);
            if (bucketIndex < 0) {
                return null;
            }
            return path.substring(bucketIndex + bucketPrefix.length());
        } catch (Exception exception) {
            log.warn("Failed to parse MinIO URL '{}': {}", fileUrl, exception.getMessage());
            return null;
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
