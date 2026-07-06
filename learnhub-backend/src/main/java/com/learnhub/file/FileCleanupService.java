
package com.learnhub.file;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileCleanupService {

    @Value("${file.cleanup.temp-dirs:}")
    private String tempDirs;

    @Value("${file.cleanup.expire-hours:24}")
    private int expireHours;

    @Scheduled(cron = "${file.cleanup.cron:0 0 * * * ?}") // Runs every hour by default
    public void cleanupExpiredFiles() {
        log.info("Starting cleanup of expired temporary files...");
        
        if (tempDirs == null || tempDirs.isBlank()) {
            // Cleanup standard Java temp directory
            cleanupDirectory(Paths.get(System.getProperty("java.io.tmpdir")));
        } else {
            for (String dir : tempDirs.split(",")) {
                cleanupDirectory(Paths.get(dir.trim()));
            }
        }
        
        log.info("Expired temporary files cleanup complete!");
    }

    private void cleanupDirectory(Path directory) {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            log.warn("Directory does not exist or is not a directory: {}", directory);
            return;
        }

        try (Stream<Path> paths = Files.walk(directory)) {
            Instant expirationTime = Instant.now().minus(Duration.ofHours(expireHours));
            
            paths.sorted(Comparator.reverseOrder())
                .filter(path -> !path.equals(directory))
                .forEach(path -> {
                    try {
                        Instant lastModified = Files.getLastModifiedTime(path).toInstant();
                        
                        if (lastModified.isBefore(expirationTime)) {
                            if (Files.isDirectory(path)) {
                                Files.delete(path);
                                log.debug("Deleted expired temp directory: {}", path);
                            } else {
                                Files.delete(path);
                                log.debug("Deleted expired temp file: {}", path);
                            }
                        }
                    } catch (IOException e) {
                        log.warn("Failed to delete path: {}", path, e);
                    }
                });
        } catch (IOException e) {
            log.error("Failed to cleanup directory: {}", directory, e);
        }
    }
}

