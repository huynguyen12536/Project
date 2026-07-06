package com.learnhub.upload.service;

import com.learnhub.upload.model.MultipartUploadSession;
import com.learnhub.upload.model.MultipartUploadStatus;
import com.learnhub.upload.repository.MultipartUploadSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MultipartUploadCleanupService {

    private final MultipartUploadSessionRepository multipartUploadSessionRepository;
    private final UploadedMediaLifecycleService uploadedMediaLifecycleService;

    @Value("${app.upload.cleanup.enabled:true}")
    private boolean cleanupEnabled;

    @Scheduled(cron = "${app.upload.cleanup.expired-session-cron:0 */15 * * * ?}")
    @Transactional
    public void cleanupExpiredSessions() {
        if (!cleanupEnabled) {
            return;
        }

        List<MultipartUploadSession> expiredSessions = multipartUploadSessionRepository.findByStatusInAndExpiresAtBefore(
            List.of(MultipartUploadStatus.INITIATED),
            LocalDateTime.now()
        );

        if (expiredSessions.isEmpty()) {
            return;
        }

        log.info("Cleaning up {} expired multipart upload session(s)", expiredSessions.size());
        uploadedMediaLifecycleService.abortExpiredSessions(expiredSessions);
    }
}
