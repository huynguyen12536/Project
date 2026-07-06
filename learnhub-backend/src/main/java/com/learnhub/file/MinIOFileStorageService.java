package com.learnhub.file;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
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

    @Override
    public FileUploadResult uploadFile(MultipartFile file, UUID userId) throws IOException {
        log.debug("Uploading avatar for user: {}", userId);
        validateFile(file);

        String extension = getFileExtension(file.getOriginalFilename());
        String objectKey = extension.isBlank()
            ? String.format("avatars/%s/avatar", userId)
            : String.format("avatars/%s/avatar.%s", userId, extension.toLowerCase());

        return uploadFileInternal(file, objectKey);
    }

    @Override
    public FileUploadResult uploadCourseFile(MultipartFile file, UUID userId, String folder) throws IOException {
        log.debug("Uploading course file for user: {} in folder: {}", userId, folder);
        validateCourseFile(file, folder);
        String extension = getFileExtension(file.getOriginalFilename());
        String fileId = UUID.randomUUID().toString();
        String objectKey = String.format("%s/%s/%s.%s", folder, userId, fileId, extension.toLowerCase());
        return uploadFileInternal(file, objectKey);
    }

    private FileUploadResult uploadFileInternal(MultipartFile file, String objectKey) throws IOException {
        // Compute SHA-256 checksum
        byte[] fileBytes = readAllBytes(file.getInputStream());
        String checksum = computeSHA256(fileBytes);

        try {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(new ByteArrayInputStream(fileBytes), fileBytes.length, -1)
                    .contentType(file.getContentType())
                    .build()
            );
        } catch (Exception exception) {
            throw new IOException("Failed to upload file to MinIO", exception);
        }

        String publicUrl = buildPublicUrl(objectKey);
        log.info("File uploaded successfully: {}, SHA-256: {}", publicUrl, checksum);
        
        return FileUploadResult.builder()
            .url(publicUrl)
            .checksum(checksum)
            .checksumAlgorithm("SHA-256")
            .fileSize((long) fileBytes.length)
            .mimeType(file.getContentType())
            .build();
    }

    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[4096];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    private String computeSHA256(byte[] data) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Failed to compute SHA-256 hash", e);
        }
    }

    private void validateCourseFile(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if ("videos".equals(folder)) {
            long maxVideoSize = 1024 * 1024 * 500; // 500 MB
            if (file.getSize() > maxVideoSize) {
                throw new IllegalArgumentException(
                    String.format("Video size exceeds maximum of %d bytes", maxVideoSize)
                );
            }
            Set<String> allowedVideoTypes = Set.of("video/mp4", "video/webm", "video/quicktime");
            String mimeType = file.getContentType();
            if (mimeType == null || !allowedVideoTypes.contains(mimeType)) {
                throw new IllegalArgumentException("Invalid video type. Allowed: MP4, WebM, QuickTime");
            }
        } else if ("thumbnails".equals(folder)) {
            validateFile(file); // uses existing image validation
        }
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
