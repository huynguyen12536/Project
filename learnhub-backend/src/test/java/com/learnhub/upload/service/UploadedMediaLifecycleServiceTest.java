package com.learnhub.upload.service;

import com.learnhub.catalog.course.model.Course;
import com.learnhub.catalog.course.model.CourseLecture;
import com.learnhub.catalog.course.repository.CourseLectureRepository;
import com.learnhub.catalog.course.repository.CourseRepository;
import com.learnhub.catalog.course.repository.CourseSectionRepository;
import com.learnhub.upload.model.MultipartUploadSession;
import com.learnhub.upload.model.MultipartUploadStatus;
import com.learnhub.upload.model.UploadAssetType;
import com.learnhub.upload.model.UploadedMediaObject;
import com.learnhub.upload.repository.MultipartUploadSessionRepository;
import com.learnhub.upload.repository.UploadedMediaObjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadedMediaLifecycleServiceTest {

    @Mock
    private UploadedMediaObjectRepository uploadedMediaObjectRepository;

    @Mock
    private MultipartUploadSessionRepository multipartUploadSessionRepository;

    @Mock
    private CourseLectureRepository courseLectureRepository;

    @Mock
    private CourseSectionRepository courseSectionRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private ObjectStorageService objectStorageService;

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private UploadedMediaLifecycleService uploadedMediaLifecycleService;

    private UUID lectureId;
    private Course course;
    private CourseLecture lecture;

    @BeforeEach
    void setUp() {
        lectureId = UUID.randomUUID();
        course = Course.builder().id(UUID.randomUUID()).build();
        lecture = CourseLecture.builder()
            .id(lectureId)
            .course(course)
            .title("Lecture")
            .type(CourseLecture.LectureType.VIDEO)
            .displayOrder(10)
            .isFreePreview(false)
            .videoUrl("http://192.168.184.128:9000/avatars/videos/old.mp4")
            .durationSeconds(99)
            .build();
    }

    @Test
    void prepareLectureVideoReplacement_abortsSessionsDeletesAssetsAndClearsLectureReference() {
        MultipartUploadSession activeSession = MultipartUploadSession.builder()
            .uploadId("upload-123")
            .objectKey("videos/active.mp4")
            .bucketName("avatars")
            .status(MultipartUploadStatus.INITIATED)
            .lectureId(lectureId)
            .expiresAt(LocalDateTime.now().plusHours(1))
            .build();

        UploadedMediaObject mediaObject = UploadedMediaObject.builder()
            .id(UUID.randomUUID())
            .bucketName("avatars")
            .objectKey("videos/old.mp4")
            .thumbnailObjectKey("video-thumbnails/old.jpg")
            .assetType(UploadAssetType.COURSE_VIDEO)
            .lectureId(lectureId)
            .publicUrl("http://192.168.184.128:9000/avatars/videos/old.mp4")
            .filename("old.mp4")
            .sizeBytes(100L)
            .contentType("video/mp4")
            .processingStatus(com.learnhub.upload.model.MediaProcessingStatus.COMPLETED)
            .uploadedBy(UUID.randomUUID())
            .build();

        when(multipartUploadSessionRepository.findByLectureIdAndStatusIn(eq(lectureId), anyList()))
            .thenReturn(List.of(activeSession));
        when(uploadedMediaObjectRepository.findByLectureIdAndAssetTypeOrderByCreatedAtDesc(lectureId, UploadAssetType.COURSE_VIDEO))
            .thenReturn(List.of(mediaObject));
        when(courseLectureRepository.findById(lectureId)).thenReturn(Optional.of(lecture));
        when(courseSectionRepository.findByCourseIdOrderByDisplayOrderAsc(course.getId())).thenReturn(List.of());

        uploadedMediaLifecycleService.prepareLectureVideoReplacement(lectureId);

        verify(objectStorageService).deleteObject("avatars", "videos/old.mp4");
        verify(objectStorageService).deleteObject("avatars", "video-thumbnails/old.jpg");
        verify(uploadedMediaObjectRepository).delete(mediaObject);
        verify(s3Client).abortMultipartUpload(any(software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest.class));
        assertThat(lecture.getVideoUrl()).isNull();
        assertThat(lecture.getDurationSeconds()).isNull();
    }
}
