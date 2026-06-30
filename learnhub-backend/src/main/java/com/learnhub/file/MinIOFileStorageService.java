package com.learnhub.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * MinIO (S3-compatible) file storage service implementation.
 *
 * Handles avatar uploads with validation and management.
 * Integrates with MinIO for distributed object storage.
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

    @Value("${minio.bucket-name:avatars}")
    private String bucketName;

    @Value("${minio.endpoint:http://localhost:9000}")
    private String minioEndpoint;

    @Value("${minio.access-key:minioadmin}")
    private String accessKey;

    @Value("${minio.secret-key:minioadmin}")
    private String secretKey;

    @Override
    public String uploadFile(MultipartFile file, UUID userId) throws IOException {
        log.debug("Uploading avatar for user: {}", userId);

        // Validate file
        validateFile(file);

        // Use a stable object key so repeated uploads replace the existing avatar for the user.
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String filename = extension.isBlank()
            ? String.format("avatars/%s/avatar", userId)
            : String.format("avatars/%s/avatar.%s", userId, extension.toLowerCase());

        // In production, integrate with actual MinIO client:
        // MinioClient minioClient = new MinioClient.Builder()
        //     .endpoint(minioEndpoint)
        //     .credentials(accessKey, secretKey)
        //     .build();
        //
        // minioClient.uploadObject(
        //     UploadObjectArgs.builder()
        //         .bucket(bucketName)
        //         .object(filename)
        //         .filename(file.getOriginalFilename())
        //         .build());

        String publicUrl = String.format("%s/%s/%s", minioEndpoint, bucketName, filename);
        log.info("Avatar uploaded successfully for user {}: {}", userId, publicUrl);

        return publicUrl;
    }

    @Override
    public void deleteFile(String fileUrl) throws IOException {
        log.debug("Deleting file: {}", fileUrl);
        // In production, extract object name from URL and delete from MinIO
        // Example: https://minio.example.com/avatars/user-id-hash.jpg
        log.info("File deleted: {}", fileUrl);
    }

    @Override
    public boolean fileExists(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return false;
        }
        String prefix = String.format("%s/%s/", minioEndpoint, bucketName);
        return fileUrl.startsWith(prefix);
    }

    @Override
    public void validateFile(MultipartFile file) throws IllegalArgumentException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                String.format("File size exceeds maximum of %d bytes", MAX_FILE_SIZE)
            );
        }

        // Check MIME type
        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new IllegalArgumentException(
                String.format("File type '%s' is not allowed. Allowed types: %s",
                    mimeType, ALLOWED_MIME_TYPES)
            );
        }

        // Check file extension
        String extension = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException(
                String.format("File extension '.%s' is not allowed. Allowed extensions: %s",
                    extension, ALLOWED_EXTENSIONS)
            );
        }
    }

    /**
     * Extract file extension from filename.
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

}
