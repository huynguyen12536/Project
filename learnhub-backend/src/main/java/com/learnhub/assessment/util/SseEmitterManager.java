package com.learnhub.assessment.util;

import com.learnhub.assessment.dto.AssessmentProgressEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;

/**
 * Thread-safe registry for managing active SSE emitter connections.
 *
 * Maintains: Map<AssessmentId, Map<UserId, SseEmitter>>
 * Enforces: Max 5 concurrent subscriptions per user
 *
 * Uses synchronized Map for thread-safety (not ConcurrentHashMap per architecture).
 * Handles client disconnections gracefully with automatic cleanup.
 */
@Component
@Slf4j
public class SseEmitterManager {

    // Registry structure: assessmentId -> (userId -> emitter)
    private final Map<UUID, Map<UUID, SseEmitter>> registry = Collections.synchronizedMap(new HashMap<>());

    private static final int MAX_CONCURRENT_SUBSCRIPTIONS_PER_USER = 5;
    private static final long SSE_TIMEOUT_MILLIS = 600_000L; // 10 minutes

    /**
     * Register a new SSE emitter for assessment progress tracking.
     *
     * @param assessmentId assessment being tracked
     * @param userId user receiving updates
     * @param emitter Spring SseEmitter for this connection
     * @throws IllegalStateException if user exceeds max concurrent subscriptions
     */
    public synchronized void register(UUID assessmentId, UUID userId, SseEmitter emitter) {
        if (!canRegister(userId)) {
            throw new IllegalStateException(
                "User " + userId + " exceeds maximum concurrent subscriptions (" + MAX_CONCURRENT_SUBSCRIPTIONS_PER_USER + ")"
            );
        }

        // Set timeout and cleanup callbacks
        emitter.onTimeout(() -> handleTimeout(assessmentId, userId));
        emitter.onCompletion(() -> handleCompletion(assessmentId, userId));

        registry.computeIfAbsent(assessmentId, k -> Collections.synchronizedMap(new HashMap<>()))
            .put(userId, emitter);

        log.debug("SSE emitter registered: assessment={}, user={}, active_connections={}",
            assessmentId, userId, getConnectionCount(userId));
    }

    /**
     * Deregister an SSE emitter.
     *
     * @param assessmentId assessment ID
     * @param userId user ID
     */
    public synchronized void deregister(UUID assessmentId, UUID userId) {
        Map<UUID, SseEmitter> emitters = registry.get(assessmentId);
        if (emitters != null) {
            emitters.remove(userId);
            if (emitters.isEmpty()) {
                registry.remove(assessmentId);
            }
            log.debug("SSE emitter deregistered: assessment={}, user={}", assessmentId, userId);
        }
    }

    /**
     * Broadcast progress event to all registered clients for an assessment.
     *
     * @param assessmentId assessment ID
     * @param event progress event to emit
     */
    public void emit(UUID assessmentId, AssessmentProgressEvent event) {
        Map<UUID, SseEmitter> emitters = registry.get(assessmentId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        List<UUID> failedEmitters = new ArrayList<>();

        for (Map.Entry<UUID, SseEmitter> entry : emitters.entrySet()) {
            UUID userId = entry.getKey();
            SseEmitter emitter = entry.getValue();

            try {
                SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
                    .id(UUID.randomUUID().toString())
                    .name("status")
                    .data(event)
                    .reconnectTime(2000L); // 2 second reconnect time

                emitter.send(eventBuilder);

                log.trace("SSE event sent: assessment={}, user={}, status={}, progress={}%",
                    assessmentId, userId, event.status(), event.progressPercent());

            } catch (IOException e) {
                log.debug("SSE emit failed for user {}: {}", userId, e.getMessage());
                failedEmitters.add(userId);
            }
        }

        // Clean up failed connections
        for (UUID userId : failedEmitters) {
            deregister(assessmentId, userId);
        }
    }

    /**
     * Check if a user can register another SSE subscription.
     *
     * @param userId user ID
     * @return true if user is below max concurrent subscriptions
     */
    public synchronized boolean canRegister(UUID userId) {
        return getConnectionCount(userId) < MAX_CONCURRENT_SUBSCRIPTIONS_PER_USER;
    }

    /**
     * Get count of active SSE connections for a user across all assessments.
     *
     * @param userId user ID
     * @return count of active connections
     */
    public synchronized int getConnectionCount(UUID userId) {
        int count = 0;
        for (Map<UUID, SseEmitter> emitters : registry.values()) {
            if (emitters.containsKey(userId)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Handle SSE timeout (10 minutes of inactivity).
     * Automatically cleans up the connection.
     */
    private void handleTimeout(UUID assessmentId, UUID userId) {
        log.debug("SSE connection timeout: assessment={}, user={}", assessmentId, userId);
        deregister(assessmentId, userId);
    }

    /**
     * Handle SSE completion (client closed connection).
     * Automatically cleans up the connection.
     */
    private void handleCompletion(UUID assessmentId, UUID userId) {
        log.debug("SSE connection completed: assessment={}, user={}", assessmentId, userId);
        deregister(assessmentId, userId);
    }

    /**
     * Get registry size for testing/monitoring.
     *
     * @return number of active assessments with registered emitters
     */
    public int getRegistrySize() {
        return registry.size();
    }
}
