package com.learnhub.file;

import com.learnhub.catalog.course.model.CourseLecture;
import com.learnhub.catalog.course.repository.CourseLectureRepository;
import com.learnhub.upload.model.MediaProcessingStatus;
import com.learnhub.upload.model.UploadedMediaObject;
import com.learnhub.upload.repository.UploadedMediaObjectRepository;
import com.learnhub.upload.service.ObjectStorageService;
import com.learnhub.upload.service.UploadedMediaLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoProcessingService {

    private final UploadedMediaObjectRepository uploadedMediaObjectRepository;
    private final CourseLectureRepository courseLectureRepository;
    private final ObjectStorageService objectStorageService;
    private final UploadedMediaLifecycleService uploadedMediaLifecycleService;

    @Async("uploadProcessingExecutor")
    public void processVideoAsync(UUID mediaObjectId) {
        UploadedMediaObject mediaObject = uploadedMediaObjectRepository.findById(mediaObjectId).orElse(null);
        if (mediaObject == null) {
            return;
        }

        mediaObject.setProcessingStatus(MediaProcessingStatus.PROCESSING);
        mediaObject.setMetadataError(null);
        uploadedMediaObjectRepository.save(mediaObject);

        Path tempDir = null;
        Path tempVideo = null;
        Path tempThumbnail = null;

        try {
            tempDir = Files.createTempDirectory("learnhub-upload-");
            tempVideo = tempDir.resolve("video.bin");
            tempThumbnail = tempDir.resolve("thumbnail.jpg");

            String checksum = downloadAndChecksum(mediaObject, tempVideo);
            Integer durationSeconds = probeVideoDuration(tempVideo);
            String thumbnailUrl = extractAndUploadThumbnail(mediaObject, tempVideo, tempThumbnail);

            mediaObject.setChecksumSha256(checksum);
            mediaObject.setChecksumAlgorithm("SHA-256");
            mediaObject.setVerifiedAt(LocalDateTime.now());
            mediaObject.setProcessingStatus(MediaProcessingStatus.COMPLETED);
            mediaObject.setMetadataError(null);

            if (durationSeconds != null && durationSeconds > 0) {
                mediaObject.setDurationSeconds(durationSeconds);
                updateLectureDuration(mediaObject.getLectureId(), durationSeconds);
            }

            mediaObject.setThumbnailUrl(thumbnailUrl);
            mediaObject.setThumbnailObjectKey(extractObjectKey(thumbnailUrl, mediaObject.getBucketName()));
            uploadedMediaObjectRepository.save(mediaObject);
            uploadedMediaLifecycleService.attachCompletedLectureVideo(mediaObject);
        } catch (Exception exception) {
            log.error("Failed to process uploaded media {}", mediaObjectId, exception);
            mediaObject.setProcessingStatus(MediaProcessingStatus.FAILED);
            mediaObject.setMetadataError(exception.getMessage());
            uploadedMediaObjectRepository.save(mediaObject);
        } finally {
            deleteQuietly(tempThumbnail);
            deleteQuietly(tempVideo);
            deleteQuietly(tempDir);
        }
    }

    private String downloadAndChecksum(UploadedMediaObject mediaObject, Path targetPath) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (
            InputStream source = objectStorageService.getObject(mediaObject.getBucketName(), mediaObject.getObjectKey());
            DigestInputStream digestInputStream = new DigestInputStream(source, digest)
        ) {
            Files.copy(digestInputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        return HexFormat.of().formatHex(digest.digest());
    }

    private Integer probeVideoDuration(Path videoPath) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(
            "ffprobe",
            "-v", "error",
            "-show_entries", "format=duration",
            "-of", "default=noprint_wrappers=1:nokey=1",
            videoPath.toAbsolutePath().toString()
        );

        Process process = processBuilder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String duration = reader.readLine();
            int exitCode = process.waitFor();
            if (exitCode != 0 || duration == null || duration.isBlank()) {
                return null;
            }
            return (int) Math.round(Double.parseDouble(duration.trim()));
        }
    }

    private String extractAndUploadThumbnail(UploadedMediaObject mediaObject, Path videoPath, Path thumbnailPath) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(
            "ffmpeg",
            "-y",
            "-ss", "00:00:01",
            "-i", videoPath.toAbsolutePath().toString(),
            "-vframes", "1",
            "-q:v", "2",
            thumbnailPath.toAbsolutePath().toString()
        );

        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0 || !Files.exists(thumbnailPath)) {
            throw new IOException("Khong the extract thumbnail cho video");
        }

        byte[] thumbnailBytes = Files.readAllBytes(thumbnailPath);
        if (mediaObject.getThumbnailObjectKey() != null && !mediaObject.getThumbnailObjectKey().isBlank()) {
            objectStorageService.deleteObject(mediaObject.getBucketName(), mediaObject.getThumbnailObjectKey());
        }
        String objectKey = objectStorageService.buildGeneratedObjectKey("video-thumbnails", mediaObject.getUploadedBy(), "jpg");
        return objectStorageService.putObject(mediaObject.getBucketName(), objectKey, thumbnailBytes, "image/jpeg");
    }

    private void updateLectureDuration(UUID lectureId, Integer durationSeconds) {
        if (lectureId == null || durationSeconds == null || durationSeconds <= 0) {
            return;
        }

        courseLectureRepository.findById(lectureId).ifPresent(lecture -> {
            lecture.setDurationSeconds(durationSeconds);
            courseLectureRepository.save(lecture);
        });
    }

    private String extractObjectKey(String publicUrl, String bucketName) {
        if (publicUrl == null || publicUrl.isBlank()) {
            return null;
        }

        String marker = "/" + bucketName + "/";
        int markerIndex = publicUrl.indexOf(marker);
        if (markerIndex < 0) {
            return null;
        }
        return publicUrl.substring(markerIndex + marker.length()).replaceFirst("^/+", "");
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            log.debug("Failed to delete temp path {}", path, exception);
        }
    }
}
