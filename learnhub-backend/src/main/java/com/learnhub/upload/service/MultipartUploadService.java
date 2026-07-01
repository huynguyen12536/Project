package com.learnhub.upload.service;

import com.learnhub.catalog.course.model.Course;
import com.learnhub.catalog.course.model.CourseLecture;
import com.learnhub.catalog.course.repository.CourseLectureRepository;
import com.learnhub.catalog.course.repository.CourseRepository;
import com.learnhub.upload.config.StorageProperties;
import com.learnhub.upload.config.UploadProperties;
import com.learnhub.upload.dto.request.AbortMultipartUploadRequest;
import com.learnhub.upload.dto.request.CompleteMultipartUploadRequest;
import com.learnhub.upload.dto.request.CompletedUploadPartRequest;
import com.learnhub.upload.dto.request.PresignMultipartUploadRequest;
import com.learnhub.upload.dto.request.StartMultipartUploadRequest;
import com.learnhub.upload.dto.response.CompleteMultipartUploadResponse;
import com.learnhub.upload.dto.response.MultipartUploadPresignResponse;
import com.learnhub.upload.dto.response.MultipartUploadSessionResponse;
import com.learnhub.upload.dto.response.PresignedPartResponse;
import com.learnhub.upload.dto.response.UploadedPartResponse;
import com.learnhub.upload.model.MultipartUploadSession;
import com.learnhub.upload.model.MultipartUploadStatus;
import com.learnhub.upload.model.UploadAssetType;
import com.learnhub.upload.model.UploadedMediaObject;
import com.learnhub.upload.repository.MultipartUploadSessionRepository;
import com.learnhub.upload.repository.UploadedMediaObjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.model.ListPartsResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MultipartUploadService {

    private final MultipartUploadSessionRepository sessionRepository;
    private final UploadedMediaObjectRepository uploadedMediaObjectRepository;
    private final CourseRepository courseRepository;
    private final CourseLectureRepository courseLectureRepository;
    private final StorageProperties storageProperties;
    private final UploadProperties uploadProperties;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public MultipartUploadSessionResponse startUpload(StartMultipartUploadRequest request, Authentication authentication) {
        UUID userId = currentUserId(authentication);
        UploadAssetType assetType = parseAssetType(request.assetType());
        String sanitizedFileName = sanitizeFilename(request.fileName());
        validateVideoRequest(request, sanitizedFileName, assetType);
        validateUploadOwnership(userId, request.courseId(), request.lectureId(), assetType);

        String extension = extractExtension(sanitizedFileName);
        String objectKey = buildObjectKey(userId, assetType, extension);

        CreateMultipartUploadResponse response = s3Client.createMultipartUpload(
            CreateMultipartUploadRequest.builder()
                .bucket(storageProperties.getBucketName())
                .key(objectKey)
                .contentType(request.contentType())
                .build()
        );

        MultipartUploadSession session = sessionRepository.save(
            MultipartUploadSession.builder()
                .uploadId(response.uploadId())
                .objectKey(objectKey)
                .bucketName(storageProperties.getBucketName())
                .originalFilename(sanitizedFileName)
                .contentType(request.contentType())
                .totalSize(request.size())
                .assetType(assetType)
                .status(MultipartUploadStatus.INITIATED)
                .uploadedBy(userId)
                .courseId(request.courseId())
                .lectureId(request.lectureId())
                .expiresAt(LocalDateTime.now().plusHours(uploadProperties.getSessionTtlHours()))
                .build()
        );

        return toSessionResponse(session, List.of());
    }

    @Transactional(readOnly = true)
    public MultipartUploadSessionResponse getStatus(String uploadId, String objectKey, Authentication authentication) {
        MultipartUploadSession session = getActiveOwnedSession(uploadId, objectKey, currentUserId(authentication));
        return toSessionResponse(session, listUploadedParts(session));
    }

    public MultipartUploadPresignResponse presignParts(PresignMultipartUploadRequest request, Authentication authentication) {
        MultipartUploadSession session = getActiveOwnedSession(
            request.uploadId(),
            request.objectKey(),
            currentUserId(authentication)
        );

        List<PresignedPartResponse> parts = request.partNumbers().stream()
            .distinct()
            .sorted()
            .map(partNumber -> presignPart(session, partNumber))
            .toList();

        return new MultipartUploadPresignResponse(session.getUploadId(), session.getObjectKey(), parts);
    }

    public CompleteMultipartUploadResponse completeUpload(
        CompleteMultipartUploadRequest request,
        Authentication authentication
    ) {
        MultipartUploadSession session = getOwnedSession(
            request.uploadId(),
            request.objectKey(),
            currentUserId(authentication)
        );

        if (session.getStatus() == MultipartUploadStatus.COMPLETED) {
            UploadedMediaObject existingAsset = uploadedMediaObjectRepository.findBySessionId(session.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Upload da hoan tat truoc do"));
            return toCompleteResponse(existingAsset, session);
        }

        ensureSessionMutable(session);

        List<CompletedPart> completedParts = request.parts().stream()
            .sorted(Comparator.comparingInt(CompletedUploadPartRequest::partNumber))
            .map(part -> CompletedPart.builder()
                .partNumber(part.partNumber())
                .eTag(normalizeEtag(part.eTag()))
                .build())
            .toList();

        CompletedMultipartUpload multipartUpload = CompletedMultipartUpload.builder()
            .parts(completedParts)
            .build();

        String normalizedObjectUrl = buildPublicUrl(session.getObjectKey());

        s3Client.completeMultipartUpload(
            software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest.builder()
                .bucket(session.getBucketName())
                .key(session.getObjectKey())
                .uploadId(session.getUploadId())
                .multipartUpload(multipartUpload)
                .build()
        );

        session.setStatus(MultipartUploadStatus.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());
        session.setPublicUrl(normalizedObjectUrl);
        session.setErrorMessage(null);
        MultipartUploadSession savedSession = sessionRepository.save(session);

        UploadedMediaObject mediaObject = uploadedMediaObjectRepository.findBySessionId(savedSession.getId())
            .orElseGet(() -> UploadedMediaObject.builder().sessionId(savedSession.getId()).build());

        mediaObject.setBucketName(savedSession.getBucketName());
        mediaObject.setObjectKey(savedSession.getObjectKey());
        mediaObject.setPublicUrl(normalizedObjectUrl);
        mediaObject.setFilename(savedSession.getOriginalFilename());
        mediaObject.setSizeBytes(savedSession.getTotalSize());
        mediaObject.setContentType(savedSession.getContentType());
        mediaObject.setDurationSeconds(request.durationSeconds());
        mediaObject.setAssetType(savedSession.getAssetType());
        mediaObject.setUploadedBy(savedSession.getUploadedBy());
        mediaObject.setCourseId(savedSession.getCourseId());
        mediaObject.setLectureId(savedSession.getLectureId());
        mediaObject.setEtag(completedParts.isEmpty() ? null : completedParts.get(completedParts.size() - 1).eTag());

        UploadedMediaObject savedObject = uploadedMediaObjectRepository.save(mediaObject);
        return toCompleteResponse(savedObject, savedSession);
    }

    public void abortUpload(AbortMultipartUploadRequest request, Authentication authentication) {
        MultipartUploadSession session = getOwnedSession(
            request.uploadId(),
            request.objectKey(),
            currentUserId(authentication)
        );

        if (session.getStatus() == MultipartUploadStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Khong the huy upload da hoan tat");
        }

        try {
            s3Client.abortMultipartUpload(
                software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest.builder()
                    .bucket(session.getBucketName())
                    .key(session.getObjectKey())
                    .uploadId(session.getUploadId())
                    .build()
            );
        } catch (SdkException exception) {
            log.warn("Abort multipart upload failed for uploadId={} objectKey={}: {}", session.getUploadId(), session.getObjectKey(), exception.getMessage());
        }

        session.setStatus(MultipartUploadStatus.ABORTED);
        session.setAbortedAt(LocalDateTime.now());
        sessionRepository.save(session);
    }

    private PresignedPartResponse presignPart(MultipartUploadSession session, Integer partNumber) {
        if (partNumber == null || partNumber <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "partNumber phai lon hon 0");
        }

        UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
            .bucket(session.getBucketName())
            .key(session.getObjectKey())
            .uploadId(session.getUploadId())
            .partNumber(partNumber)
            .build();

        PresignedUploadPartRequest presignedRequest = s3Presigner.presignUploadPart(
            builder -> builder
                .signatureDuration(Duration.ofMinutes(uploadProperties.getPresignExpiryMinutes()))
                .uploadPartRequest(uploadPartRequest)
        );

        return new PresignedPartResponse(partNumber, presignedRequest.url().toString());
    }

    private MultipartUploadSessionResponse toSessionResponse(
        MultipartUploadSession session,
        List<UploadedPartResponse> uploadedParts
    ) {
        return new MultipartUploadSessionResponse(
            session.getId(),
            session.getUploadId(),
            session.getObjectKey(),
            session.getBucketName(),
            session.getAssetType().name(),
            session.getOriginalFilename(),
            session.getContentType(),
            session.getTotalSize(),
            uploadProperties.getChunkSizeBytes(),
            uploadProperties.getMaxConcurrency(),
            uploadProperties.getMaxRetries(),
            session.getExpiresAt(),
            uploadedParts
        );
    }

    private CompleteMultipartUploadResponse toCompleteResponse(
        UploadedMediaObject mediaObject,
        MultipartUploadSession session
    ) {
        return new CompleteMultipartUploadResponse(
            mediaObject.getId(),
            session.getId(),
            session.getUploadId(),
            mediaObject.getObjectKey(),
            mediaObject.getBucketName(),
            mediaObject.getPublicUrl(),
            mediaObject.getContentType(),
            mediaObject.getSizeBytes(),
            mediaObject.getDurationSeconds(),
            session.getCompletedAt()
        );
    }

    private List<UploadedPartResponse> listUploadedParts(MultipartUploadSession session) {
        List<UploadedPartResponse> parts = new ArrayList<>();
        Integer marker = null;

        while (true) {
            ListPartsResponse response = s3Client.listParts(
                ListPartsRequest.builder()
                    .bucket(session.getBucketName())
                    .key(session.getObjectKey())
                    .uploadId(session.getUploadId())
                    .partNumberMarker(marker)
                    .build()
            );

            response.parts().forEach(part -> parts.add(new UploadedPartResponse(
                part.partNumber(),
                part.eTag(),
                part.size()
            )));

            if (!response.isTruncated()) {
                break;
            }
            marker = response.nextPartNumberMarker();
        }

        return parts;
    }

    private MultipartUploadSession getActiveOwnedSession(String uploadId, String objectKey, UUID userId) {
        MultipartUploadSession session = getOwnedSession(uploadId, objectKey, userId);
        ensureSessionMutable(session);
        return session;
    }

    private MultipartUploadSession getOwnedSession(String uploadId, String objectKey, UUID userId) {
        return sessionRepository.findByUploadIdAndObjectKeyAndUploadedBy(uploadId, objectKey, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay upload session"));
    }

    private void ensureSessionMutable(MultipartUploadSession session) {
        if (session.getStatus() == MultipartUploadStatus.ABORTED) {
            throw new ResponseStatusException(HttpStatus.GONE, "Upload session da bi huy");
        }
        if (session.getStatus() == MultipartUploadStatus.FAILED) {
            throw new ResponseStatusException(HttpStatus.GONE, "Upload session khong con hop le");
        }
        if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(LocalDateTime.now())) {
            session.setStatus(MultipartUploadStatus.FAILED);
            session.setErrorMessage("Upload session da het han");
            sessionRepository.save(session);
            throw new ResponseStatusException(HttpStatus.GONE, "Upload session da het han");
        }
    }

    private void validateVideoRequest(
        StartMultipartUploadRequest request,
        String sanitizedFileName,
        UploadAssetType assetType
    ) {
        if (assetType != UploadAssetType.COURSE_VIDEO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loai asset khong duoc ho tro");
        }

        String contentType = request.contentType().toLowerCase(Locale.ROOT);
        if (!uploadProperties.getAllowedVideoMimeTypesList().contains(contentType)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Loai video khong hop le. Chi ho tro " + String.join(", ", uploadProperties.getAllowedVideoMimeTypesList())
            );
        }

        String extension = extractExtension(sanitizedFileName);
        if (extension.isBlank() || !uploadProperties.getAllowedVideoExtensionsList().contains(extension)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Dinh dang video khong hop le. Chi ho tro " + String.join(", ", uploadProperties.getAllowedVideoExtensionsList())
            );
        }

        if (request.size() > uploadProperties.getMaxVideoSizeBytes()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Video vuot qua gioi han toi da " + uploadProperties.getMaxVideoSizeBytes() + " bytes"
            );
        }
    }

    private void validateUploadOwnership(UUID userId, UUID courseId, UUID lectureId, UploadAssetType assetType) {
        if (assetType != UploadAssetType.COURSE_VIDEO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loai asset khong duoc ho tro");
        }

        Course course = courseRepository.findByIdAndInstructorId(courseId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Ban khong duoc phep upload cho khoa hoc nay"));

        if (lectureId == null) {
            return;
        }

        CourseLecture lecture = courseLectureRepository.findById(lectureId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay bai giang"));

        if (!lecture.getCourse().getId().equals(course.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bai giang khong thuoc khoa hoc hien tai");
        }
    }

    private UploadAssetType parseAssetType(String assetType) {
        try {
            return UploadAssetType.valueOf(assetType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loai asset khong hop le");
        }
    }

    private String buildObjectKey(UUID userId, UploadAssetType assetType, String extension) {
        String prefix = switch (assetType) {
            case COURSE_VIDEO -> "videos";
        };

        LocalDateTime now = LocalDateTime.now();
        return "%s/%s/%d/%02d/%s.%s".formatted(
            prefix,
            userId,
            now.getYear(),
            now.getMonthValue(),
            UUID.randomUUID(),
            extension
        );
    }

    private String buildPublicUrl(String objectKey) {
        return stripTrailingSlash(storageProperties.getPublicEndpoint())
            + "/"
            + storageProperties.getBucketName()
            + "/"
            + objectKey;
    }

    private String sanitizeFilename(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ten tep khong hop le");
        }

        String normalized = trimmed.replace("\\", "/");
        String lastSegment = normalized.substring(normalized.lastIndexOf('/') + 1);
        return lastSegment.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeEtag(String etag) {
        String trimmed = etag.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed;
        }
        return "\"" + trimmed.replace("\"", "") + "\"";
    }

    private UUID currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Ban can dang nhap");
        }
        return UUID.fromString(authentication.getName());
    }

    private String stripTrailingSlash(String value) {
        return value != null && value.endsWith("/")
            ? value.substring(0, value.length() - 1)
            : value;
    }
}
