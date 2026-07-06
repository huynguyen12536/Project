package com.learnhub.upload.service;

import com.learnhub.catalog.course.model.Course;
import com.learnhub.catalog.course.model.CourseLecture;
import com.learnhub.catalog.course.model.CourseSection;
import com.learnhub.catalog.course.repository.CourseLectureRepository;
import com.learnhub.catalog.course.repository.CourseRepository;
import com.learnhub.catalog.course.repository.CourseSectionRepository;
import com.learnhub.common.exception.ResourceNotFoundException;
import com.learnhub.upload.model.MultipartUploadSession;
import com.learnhub.upload.model.MultipartUploadStatus;
import com.learnhub.upload.model.UploadAssetType;
import com.learnhub.upload.model.UploadedMediaObject;
import com.learnhub.upload.repository.MultipartUploadSessionRepository;
import com.learnhub.upload.repository.UploadedMediaObjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadedMediaLifecycleService {

    private final UploadedMediaObjectRepository uploadedMediaObjectRepository;
    private final MultipartUploadSessionRepository multipartUploadSessionRepository;
    private final CourseLectureRepository courseLectureRepository;
    private final CourseSectionRepository courseSectionRepository;
    private final CourseRepository courseRepository;
    private final ObjectStorageService objectStorageService;
    private final S3Client s3Client;

    @Transactional
    public void prepareLectureVideoReplacement(UUID lectureId) {
        abortActiveSessionsForLecture(lectureId);
        deleteLectureVideoAssets(lectureId, null, null);
        clearLectureVideoReference(lectureId);
    }

    @Transactional
    public void attachCompletedLectureVideo(UploadedMediaObject mediaObject) {
        if (mediaObject.getLectureId() == null) {
            return;
        }

        CourseLecture lecture = courseLectureRepository.findById(mediaObject.getLectureId())
            .orElseThrow(() -> new ResourceNotFoundException("Lecture not found: " + mediaObject.getLectureId()));

        lecture.setVideoUrl(mediaObject.getPublicUrl());
        if (mediaObject.getDurationSeconds() != null) {
            lecture.setDurationSeconds(mediaObject.getDurationSeconds());
        }
        courseLectureRepository.save(lecture);
        updateCourseStats(lecture.getCourse());
    }

    @Transactional
    public void deleteLectureVideoAssets(UUID lectureId, String keepBucketName, String keepObjectKey) {
        List<UploadedMediaObject> mediaObjects = uploadedMediaObjectRepository
            .findByLectureIdAndAssetTypeOrderByCreatedAtDesc(lectureId, UploadAssetType.COURSE_VIDEO);

        for (UploadedMediaObject mediaObject : mediaObjects) {
            boolean shouldKeep = keepBucketName != null
                && keepObjectKey != null
                && Objects.equals(mediaObject.getBucketName(), keepBucketName)
                && Objects.equals(mediaObject.getObjectKey(), keepObjectKey);

            if (shouldKeep) {
                continue;
            }

            deleteStoredMediaObject(mediaObject);
            uploadedMediaObjectRepository.delete(mediaObject);
        }
    }

    @Transactional
    public void deleteLectureAndSectionAssets(UUID sectionId) {
        List<CourseLecture> lectures = courseLectureRepository.findBySectionIdOrderByDisplayOrderAsc(sectionId);
        for (CourseLecture lecture : lectures) {
            abortActiveSessionsForLecture(lecture.getId());
            deleteLectureVideoAssets(lecture.getId(), null, null);
        }
    }

    @Transactional
    public void clearLectureVideoReference(UUID lectureId) {
        courseLectureRepository.findById(lectureId).ifPresent(lecture -> {
            lecture.setVideoUrl(null);
            lecture.setDurationSeconds(null);
            courseLectureRepository.save(lecture);
            updateCourseStats(lecture.getCourse());
        });
    }

    @Transactional
    public void abortExpiredSessions(List<MultipartUploadSession> sessions) {
        for (MultipartUploadSession session : sessions) {
            try {
                s3Client.abortMultipartUpload(
                    software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest.builder()
                        .bucket(session.getBucketName())
                        .key(session.getObjectKey())
                        .uploadId(session.getUploadId())
                        .build()
                );
            } catch (SdkException exception) {
                log.warn("Failed to abort expired multipart session {}: {}", session.getUploadId(), exception.getMessage());
            }

            session.setStatus(MultipartUploadStatus.FAILED);
            session.setAbortedAt(LocalDateTime.now());
            session.setErrorMessage("Upload session expired before completion");
            multipartUploadSessionRepository.save(session);
        }
    }

    private void abortActiveSessionsForLecture(UUID lectureId) {
        List<MultipartUploadSession> activeSessions = multipartUploadSessionRepository.findByLectureIdAndStatusIn(
            lectureId,
            List.of(MultipartUploadStatus.INITIATED)
        );

        for (MultipartUploadSession session : activeSessions) {
            try {
                s3Client.abortMultipartUpload(
                    software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest.builder()
                        .bucket(session.getBucketName())
                        .key(session.getObjectKey())
                        .uploadId(session.getUploadId())
                        .build()
                );
            } catch (SdkException exception) {
                log.warn("Failed to abort active multipart session {} for lecture {}: {}", session.getUploadId(), lectureId, exception.getMessage());
            }

            session.setStatus(MultipartUploadStatus.ABORTED);
            session.setAbortedAt(LocalDateTime.now());
            session.setErrorMessage("Superseded by a newer upload");
            multipartUploadSessionRepository.save(session);
        }
    }

    private void deleteStoredMediaObject(UploadedMediaObject mediaObject) {
        deleteObjectQuietly(mediaObject.getBucketName(), mediaObject.getObjectKey());

        if (mediaObject.getThumbnailObjectKey() != null && !mediaObject.getThumbnailObjectKey().isBlank()) {
            deleteObjectQuietly(mediaObject.getBucketName(), mediaObject.getThumbnailObjectKey());
        }
    }

    private void deleteObjectQuietly(String bucketName, String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }

        try {
            objectStorageService.deleteObject(bucketName, objectKey);
        } catch (Exception exception) {
            throw new IllegalStateException(
                "Khong the xoa object storage asset truoc khi thay the: " + objectKey,
                exception
            );
        }
    }

    private void updateCourseStats(Course course) {
        List<CourseSection> sections = courseSectionRepository.findByCourseIdOrderByDisplayOrderAsc(course.getId());
        int lectureCount = 0;
        int totalSeconds = 0;

        for (CourseSection section : sections) {
            List<CourseLecture> lectures = courseLectureRepository.findBySectionIdOrderByDisplayOrderAsc(section.getId());
            lectureCount += lectures.size();
            for (CourseLecture lecture : lectures) {
                if (lecture.getDurationSeconds() != null) {
                    totalSeconds += lecture.getDurationSeconds();
                }
            }
        }

        course.setLectureCount(lectureCount);
        course.setTotalVideoDurationSeconds(totalSeconds);
        courseRepository.save(course);
    }
}
