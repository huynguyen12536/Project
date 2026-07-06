package com.learnhub.upload.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@Getter
@Setter
public class UploadProperties {

    @Value("${app.upload.multipart.chunk-size-bytes:67108864}")
    private long chunkSizeBytes;

    @Value("${app.upload.multipart.max-concurrency:6}")
    private int maxConcurrency;

    @Value("${app.upload.multipart.max-retries:3}")
    private int maxRetries;

    @Value("${app.upload.multipart.presign-expiry-minutes:20}")
    private long presignExpiryMinutes;

    @Value("${app.upload.multipart.session-ttl-hours:24}")
    private long sessionTtlHours;

    @Value("${app.upload.video.max-size-bytes:5368709120}")
    private long maxVideoSizeBytes;

    @Value("${app.upload.video.allowed-mime-types:video/mp4,video/webm,video/quicktime}")
    private String allowedVideoMimeTypes;

    @Value("${app.upload.video.allowed-extensions:mp4,webm,mov}")
    private String allowedVideoExtensions;

    public List<String> getAllowedVideoMimeTypesList() {
        return splitCsv(allowedVideoMimeTypes);
    }

    public List<String> getAllowedVideoExtensionsList() {
        return splitCsv(allowedVideoExtensions);
    }

    private List<String> splitCsv(String value) {
        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(item -> !item.isBlank())
            .map(String::toLowerCase)
            .toList();
    }
}
