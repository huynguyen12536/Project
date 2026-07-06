package com.learnhub.file;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * Service interface for file storage operations.
 *
 * Abstraction layer over MinIO/S3-compatible storage.
 * Enables easy switching between different storage backends.
 */
public interface FileStorageService {

    /**
     * Upload a file to storage.
     *
     * @param file the multipart file to upload
     * @param userId the user ID (used for organizing storage)
     * @return the FileUploadResult with URL and checksum information
     * @throws IOException if upload fails
     * @throws IllegalArgumentException if file is invalid
     */
    FileUploadResult uploadFile(MultipartFile file, UUID userId) throws IOException;

    /**
     * Upload a course-scoped asset such as a thumbnail.
     *
     * @param file the multipart file to upload
     * @param userId the uploader user ID
     * @param folder logical storage folder
     * @return the FileUploadResult with URL and checksum information
     * @throws IOException if upload fails
     */
    FileUploadResult uploadCourseFile(MultipartFile file, UUID userId, String folder) throws IOException;

    /**
     * Delete a file from storage.
     *
     * @param fileUrl the public URL of the file to delete
     * @throws IOException if deletion fails
     */
    void deleteFile(String fileUrl) throws IOException;

    /**
     * Check if a file exists in storage.
     *
     * @param fileUrl the public URL to check
     * @return true if file exists
     */
    boolean fileExists(String fileUrl);

    /**
     * Validate file type and size constraints.
     *
     * @param file the multipart file to validate
     * @throws IllegalArgumentException if validation fails
     */
    void validateFile(MultipartFile file) throws IllegalArgumentException;
}
