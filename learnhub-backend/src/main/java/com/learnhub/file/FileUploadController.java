package com.learnhub.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
    public ResponseEntity<FileUploadResponse> uploadThumbnail(
        @RequestParam("file") MultipartFile file,
        Authentication authentication
    ) throws IOException {
        UUID userId = UUID.fromString(authentication.getName());
        String fileUrl = fileStorageService.uploadCourseFile(file, userId, "thumbnails");
        return ResponseEntity.ok(FileUploadResponse.builder()
            .url(fileUrl)
            .build());
    }
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class FileUploadResponse {
    private String url;
}
