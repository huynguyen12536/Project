package com.learnhub.upload.controller;

import com.learnhub.upload.dto.request.AbortMultipartUploadRequest;
import com.learnhub.upload.dto.request.CompleteMultipartUploadRequest;
import com.learnhub.upload.dto.request.PresignMultipartUploadRequest;
import com.learnhub.upload.dto.request.StartMultipartUploadRequest;
import com.learnhub.upload.dto.response.CompleteMultipartUploadResponse;
import com.learnhub.upload.dto.response.MultipartUploadPresignResponse;
import com.learnhub.upload.dto.response.MultipartUploadSessionResponse;
import com.learnhub.upload.service.MultipartUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/uploads/multipart")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
public class MultipartUploadController {

    private final MultipartUploadService multipartUploadService;

    @PostMapping("/start")
    public MultipartUploadSessionResponse startUpload(
        @Valid @RequestBody StartMultipartUploadRequest request,
        Authentication authentication
    ) {
        return multipartUploadService.startUpload(request, authentication);
    }

    @PostMapping("/presign")
    public MultipartUploadPresignResponse presignParts(
        @Valid @RequestBody PresignMultipartUploadRequest request,
        Authentication authentication
    ) {
        return multipartUploadService.presignParts(request, authentication);
    }

    @PostMapping("/complete")
    public CompleteMultipartUploadResponse completeUpload(
        @Valid @RequestBody CompleteMultipartUploadRequest request,
        Authentication authentication
    ) {
        return multipartUploadService.completeUpload(request, authentication);
    }

    @PostMapping("/abort")
    public void abortUpload(
        @Valid @RequestBody AbortMultipartUploadRequest request,
        Authentication authentication
    ) {
        multipartUploadService.abortUpload(request, authentication);
    }

    @GetMapping("/status")
    public MultipartUploadSessionResponse getStatus(
        @RequestParam("uploadId") String uploadId,
        @RequestParam("objectKey") String objectKey,
        Authentication authentication
    ) {
        return multipartUploadService.getStatus(uploadId, objectKey, authentication);
    }
}
