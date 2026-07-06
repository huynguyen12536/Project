package com.learnhub.upload.service;

import com.learnhub.catalog.course.model.Course;
import com.learnhub.catalog.course.model.CourseLecture;
import com.learnhub.catalog.course.repository.CourseLectureRepository;
import com.learnhub.catalog.course.repository.CourseRepository;
import com.learnhub.file.VideoProcessingService;
import com.learnhub.upload.config.StorageProperties;
import com.learnhub.upload.config.UploadProperties;
import com.learnhub.upload.dto.request.AbortMultipartUploadRequest;
import com.learnhub.upload.dto.request.CompleteMultipartUploadRequest;
import com.learnhub.upload.dto.request.CompletedUploadPartRequest;
import com.learnhub.upload.dto.request.StartMultipartUploadRequest;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MultipartUploadServiceTest {

    @Mock
    private MultipartUploadSessionRepository sessionRepository;

    @Mock
    private UploadedMediaObjectRepository uploadedMediaObjectRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseLectureRepository courseLectureRepository;

    @Mock
    private StorageProperties storageProperties;

    @Mock
    private UploadProperties uploadProperties;

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private ObjectStorageService objectStorageService;

    @Mock
    private UploadedMediaLifecycleService uploadedMediaLifecycleService;

    @Mock
    private VideoProcessingService videoProcessingService;

    @InjectMocks
    private MultipartUploadService multipartUploadService;

    private UUID instructorId;
    private UUID courseId;
    private UUID lectureId;
    private UsernamePasswordAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        instructorId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        lectureId = UUID.randomUUID();
        authentication = new UsernamePasswordAuthenticationToken(
            instructorId.toString(),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"))
        );

        when(storageProperties.getBucketName()).thenReturn("avatars");
        when(uploadProperties.getChunkSizeBytes()).thenReturn(10L * 1024 * 1024);
        when(uploadProperties.getMaxConcurrency()).thenReturn(5);
        when(uploadProperties.getMaxRetries()).thenReturn(3);
        when(uploadProperties.getPresignExpiryMinutes()).thenReturn(20L);
        when(uploadProperties.getSessionTtlHours()).thenReturn(24L);
        when(uploadProperties.getMaxVideoSizeBytes()).thenReturn(5L * 1024 * 1024 * 1024);
        when(uploadProperties.getAllowedVideoMimeTypesList()).thenReturn(List.of("video/mp4", "video/webm", "video/quicktime"));
        when(uploadProperties.getAllowedVideoExtensionsList()).thenReturn(List.of("mp4", "webm", "mov"));
        when(objectStorageService.buildPublicUrl(any(), any())).thenAnswer(invocation -> {
            String bucket = invocation.getArgument(0, String.class);
            String key = invocation.getArgument(1, String.class);
            return "http://192.168.184.128:9000/" + bucket + "/" + key;
        });
    }

    @Test
    void startUpload_preparesLectureReplacementBeforeSessionCreation() {
        Course course = Course.builder().id(courseId).build();
        CourseLecture lecture = CourseLecture.builder()
            .id(lectureId)
            .course(course)
            .section(null)
            .title("Lecture")
            .type(CourseLecture.LectureType.VIDEO)
            .displayOrder(10)
            .isFreePreview(false)
            .build();

        when(courseRepository.findByIdAndInstructorId(courseId, instructorId)).thenReturn(Optional.of(course));
        when(courseLectureRepository.findById(lectureId)).thenReturn(Optional.of(lecture));
        when(s3Client.createMultipartUpload(any(software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest.class)))
            .thenReturn(CreateMultipartUploadResponse.builder().uploadId("upload-123").build());
        when(sessionRepository.save(any(MultipartUploadSession.class))).thenAnswer(invocation -> {
            MultipartUploadSession session = invocation.getArgument(0);
            session.setId(UUID.randomUUID());
            return session;
        });

        StartMultipartUploadRequest request = new StartMultipartUploadRequest(
            "lesson.mp4",
            "video/mp4",
            128L,
            "COURSE_VIDEO",
            courseId,
            lectureId
        );

        var response = multipartUploadService.startUpload(request, authentication);

        verify(uploadedMediaLifecycleService).prepareLectureVideoReplacement(lectureId);
        assertThat(response.uploadId()).isEqualTo("upload-123");
        assertThat(response.objectKey()).endsWith(".mp4");
    }

    @Test
    void completeUpload_verifiesObjectAndTriggersAsyncProcessing() {
        MultipartUploadSession session = MultipartUploadSession.builder()
            .id(UUID.randomUUID())
            .uploadId("upload-123")
            .objectKey("videos/instructor/demo.mp4")
            .bucketName("avatars")
            .originalFilename("lesson.mp4")
            .contentType("video/mp4")
            .totalSize(256L)
            .assetType(UploadAssetType.COURSE_VIDEO)
            .status(MultipartUploadStatus.INITIATED)
            .uploadedBy(instructorId)
            .courseId(courseId)
            .lectureId(lectureId)
            .expiresAt(LocalDateTime.now().plusHours(1))
            .build();

        when(sessionRepository.findByUploadIdAndObjectKeyAndUploadedBy("upload-123", "videos/instructor/demo.mp4", instructorId))
            .thenReturn(Optional.of(session));
        when(objectStorageService.headObject("avatars", "videos/instructor/demo.mp4"))
            .thenReturn(
                HeadObjectResponse.builder()
                    .contentLength(256L)
                    .contentType("video/mp4")
                    .eTag("\"etag-final\"")
                    .build()
            );
        when(uploadedMediaObjectRepository.findBySessionId(session.getId())).thenReturn(Optional.empty());
        when(uploadedMediaObjectRepository.save(any(UploadedMediaObject.class))).thenAnswer(invocation -> {
            UploadedMediaObject mediaObject = invocation.getArgument(0);
            if (mediaObject.getId() == null) {
                mediaObject.setId(UUID.randomUUID());
            }
            return mediaObject;
        });

        CompleteMultipartUploadRequest request = new CompleteMultipartUploadRequest(
            "upload-123",
            "videos/instructor/demo.mp4",
            List.of(new CompletedUploadPartRequest(1, "\"part-etag\"")),
            42
        );

        var response = multipartUploadService.completeUpload(request, authentication);

        verify(s3Client).completeMultipartUpload(any(software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest.class));
        verify(uploadedMediaLifecycleService).attachCompletedLectureVideo(any(UploadedMediaObject.class));
        verify(videoProcessingService).processVideoAsync(any(UUID.class));
        assertThat(response.publicUrl()).contains("videos/instructor/demo.mp4");
    }

    @Test
    void completeUpload_acceptsGenericContentTypeWhenObjectSizeMatches() {
        MultipartUploadSession session = MultipartUploadSession.builder()
            .id(UUID.randomUUID())
            .uploadId("upload-123")
            .objectKey("videos/instructor/demo.mp4")
            .bucketName("avatars")
            .originalFilename("lesson.mp4")
            .contentType("video/mp4")
            .totalSize(256L)
            .assetType(UploadAssetType.COURSE_VIDEO)
            .status(MultipartUploadStatus.INITIATED)
            .uploadedBy(instructorId)
            .courseId(courseId)
            .lectureId(lectureId)
            .expiresAt(LocalDateTime.now().plusHours(1))
            .build();

        when(sessionRepository.findByUploadIdAndObjectKeyAndUploadedBy("upload-123", "videos/instructor/demo.mp4", instructorId))
            .thenReturn(Optional.of(session));
        when(objectStorageService.headObject("avatars", "videos/instructor/demo.mp4"))
            .thenReturn(
                HeadObjectResponse.builder()
                    .contentLength(256L)
                    .contentType("application/octet-stream")
                    .eTag("\"etag-final\"")
                    .build()
            );
        when(uploadedMediaObjectRepository.findBySessionId(session.getId())).thenReturn(Optional.empty());
        when(uploadedMediaObjectRepository.save(any(UploadedMediaObject.class))).thenAnswer(invocation -> {
            UploadedMediaObject mediaObject = invocation.getArgument(0);
            if (mediaObject.getId() == null) {
                mediaObject.setId(UUID.randomUUID());
            }
            return mediaObject;
        });

        CompleteMultipartUploadRequest request = new CompleteMultipartUploadRequest(
            "upload-123",
            "videos/instructor/demo.mp4",
            List.of(new CompletedUploadPartRequest(1, "\"part-etag\"")),
            42
        );

        var response = multipartUploadService.completeUpload(request, authentication);

        verify(videoProcessingService).processVideoAsync(any(UUID.class));
        assertThat(response.publicUrl()).contains("videos/instructor/demo.mp4");
    }

    @Test
    void completeUpload_failsWhenStoredObjectMetadataDoesNotMatch() {
        MultipartUploadSession session = MultipartUploadSession.builder()
            .id(UUID.randomUUID())
            .uploadId("upload-123")
            .objectKey("videos/instructor/demo.mp4")
            .bucketName("avatars")
            .originalFilename("lesson.mp4")
            .contentType("video/mp4")
            .totalSize(256L)
            .assetType(UploadAssetType.COURSE_VIDEO)
            .status(MultipartUploadStatus.INITIATED)
            .uploadedBy(instructorId)
            .courseId(courseId)
            .lectureId(lectureId)
            .expiresAt(LocalDateTime.now().plusHours(1))
            .build();

        when(sessionRepository.findByUploadIdAndObjectKeyAndUploadedBy("upload-123", "videos/instructor/demo.mp4", instructorId))
            .thenReturn(Optional.of(session));
        when(objectStorageService.headObject("avatars", "videos/instructor/demo.mp4"))
            .thenReturn(
                HeadObjectResponse.builder()
                    .contentLength(1024L)
                    .contentType("video/mp4")
                    .build()
            );

        CompleteMultipartUploadRequest request = new CompleteMultipartUploadRequest(
            "upload-123",
            "videos/instructor/demo.mp4",
            List.of(new CompletedUploadPartRequest(1, "\"part-etag\"")),
            42
        );

        assertThatThrownBy(() -> multipartUploadService.completeUpload(request, authentication))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("502 BAD_GATEWAY");

        verify(objectStorageService).deleteObject("avatars", "videos/instructor/demo.mp4");
    }

    @Test
    void abortUpload_marksSessionAborted() {
        MultipartUploadSession session = MultipartUploadSession.builder()
            .id(UUID.randomUUID())
            .uploadId("upload-123")
            .objectKey("videos/instructor/demo.mp4")
            .bucketName("avatars")
            .status(MultipartUploadStatus.INITIATED)
            .uploadedBy(instructorId)
            .expiresAt(LocalDateTime.now().plusHours(1))
            .build();

        when(sessionRepository.findByUploadIdAndObjectKeyAndUploadedBy("upload-123", "videos/instructor/demo.mp4", instructorId))
            .thenReturn(Optional.of(session));

        multipartUploadService.abortUpload(new AbortMultipartUploadRequest("upload-123", "videos/instructor/demo.mp4"), authentication);

        ArgumentCaptor<MultipartUploadSession> captor = ArgumentCaptor.forClass(MultipartUploadSession.class);
        verify(sessionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(MultipartUploadStatus.ABORTED);
    }
}
