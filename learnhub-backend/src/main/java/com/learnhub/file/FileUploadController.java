package com.learnhub.file;

import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files")
@AllArgsConstructor
public class FileUploadController {
    private final FileStorageService fileStorageService;

    @PostMapping(value = "/upload-thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResult> uploadThumbnail(
        @RequestParam("file") MultipartFile file,
        Authentication authentication
    ) throws IOException {
        UUID userId = UUID.fromString(authentication.getName());
        FileUploadResult result = fileStorageService.uploadCourseFile(file, userId, "thumbnails");
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/upload-video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResult> uploadVideo(
        @RequestParam("file") MultipartFile file,
        Authentication authentication
    ) throws IOException {
        UUID userId = UUID.fromString(authentication.getName());
        FileUploadResult result = fileStorageService.uploadCourseFile(file, userId, "videos");
        return ResponseEntity.ok(result);
    }
}
