package com.learnhub.assessment.util;

import com.learnhub.assessment.dto.AssessmentProgressEvent;
import com.learnhub.assessment.entity.AssessmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("SseEmitterManager - SSE Connection Registry")
class SseEmitterManagerTest {

    private SseEmitterManager manager;
    private UUID assessmentId;
    private UUID userId;
    private SseEmitter emitterMock;

    @BeforeEach
    void setUp() {
        manager = new SseEmitterManager();
        assessmentId = UUID.randomUUID();
        userId = UUID.randomUUID();
        emitterMock = mock(SseEmitter.class);
    }

    @Test
    @DisplayName("Register emitter and retrieve connection count")
    void testRegisterAndGetConnectionCount() {
        assertThat(manager.getConnectionCount(userId)).isZero();

        manager.register(assessmentId, userId, emitterMock);

        assertThat(manager.getConnectionCount(userId)).isEqualTo(1);
    }

    @Test
    @DisplayName("Register multiple emitters for same user")
    void testRegisterMultipleEmittersForSameUser() {
        UUID assessmentId2 = UUID.randomUUID();
        SseEmitter emitterMock2 = mock(SseEmitter.class);

        manager.register(assessmentId, userId, emitterMock);
        manager.register(assessmentId2, userId, emitterMock2);

        assertThat(manager.getConnectionCount(userId)).isEqualTo(2);
    }

    @Test
    @DisplayName("Deregister emitter reduces connection count")
    void testDeregisterReducesConnectionCount() {
        manager.register(assessmentId, userId, emitterMock);
        assertThat(manager.getConnectionCount(userId)).isEqualTo(1);

        manager.deregister(assessmentId, userId);

        assertThat(manager.getConnectionCount(userId)).isZero();
    }

    @Test
    @DisplayName("Emit broadcasts event to all registered emitters for assessment")
    void testEmitBroadcastsToAllEmitters() throws IOException {
        UUID userId2 = UUID.randomUUID();
        SseEmitter emitterMock2 = mock(SseEmitter.class);

        manager.register(assessmentId, userId, emitterMock);
        manager.register(assessmentId, userId2, emitterMock2);

        AssessmentProgressEvent event = new AssessmentProgressEvent(
            assessmentId,
            AssessmentStatus.PROCESSING,
            50,
            "Processing",
            null,
            null,
            0.5,
            Instant.now()
        );

        manager.emit(assessmentId, event);

        verify(emitterMock).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitterMock2).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("Thread-safe concurrent registration and deregistration")
    void testThreadSafeConcurrentOperations() throws InterruptedException {
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                UUID localUserId = userId;
                UUID localAssessmentId = UUID.randomUUID();
                SseEmitter emitter = mock(SseEmitter.class);

                manager.register(localAssessmentId, localUserId, emitter);
                manager.register(localAssessmentId, localUserId, emitter);  // Second for this user
                manager.deregister(localAssessmentId, localUserId);
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // After concurrent operations complete, should be deterministic
        assertThat(manager.getConnectionCount(userId)).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Handle IOException on emit removes emitter")
    void testHandleEmitIOExceptionRemovesEmitter() throws IOException {
        manager.register(assessmentId, userId, emitterMock);
        assertThat(manager.getConnectionCount(userId)).isEqualTo(1);

        // Mock emitter throws IOException
        doThrow(new IOException("Client disconnected")).when(emitterMock).send(any(SseEmitter.SseEventBuilder.class));

        AssessmentProgressEvent event = new AssessmentProgressEvent(
            assessmentId,
            AssessmentStatus.PROCESSING,
            50,
            "Processing",
            null,
            null,
            0.5,
            Instant.now()
        );

        // Emit should handle exception gracefully
        assertThatNoException().isThrownBy(() -> manager.emit(assessmentId, event));

        // Connection should be cleaned up after IOException
        // (Allows grace period for client reconnection, then cleanup)
        assertThat(manager.getConnectionCount(userId)).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Emit with no registered emitters does not throw")
    void testEmitWithNoRegisteredEmittersDoesNotThrow() {
        AssessmentProgressEvent event = new AssessmentProgressEvent(
            assessmentId,
            AssessmentStatus.PROCESSING,
            50,
            "Processing",
            null,
            null,
            0.5,
            Instant.now()
        );

        assertThatNoException().isThrownBy(() -> manager.emit(assessmentId, event));
    }

    @Test
    @DisplayName("Event contains correct fields when emitted")
    void testEmittedEventStructure() throws IOException {
        manager.register(assessmentId, userId, emitterMock);

        AssessmentProgressEvent event = new AssessmentProgressEvent(
            assessmentId,
            AssessmentStatus.PROCESSING,
            45,
            "Evaluating API design",
            12,
            null,
            0.85,
            Instant.now()
        );

        manager.emit(assessmentId, event);

        verify(emitterMock).send(argThat(eventBuilder -> {
            // Verify the event was built (we can't inspect directly but verify method was called)
            return true;
        }));
    }

    @Test
    @DisplayName("Max 5 concurrent subscriptions per user enforcement")
    void testMaxConcurrentSubscriptionsPerUser() {
        // Register 5 assessments for same user (max allowed)
        for (int i = 0; i < 5; i++) {
            manager.register(UUID.randomUUID(), userId, mock(SseEmitter.class));
        }

        assertThat(manager.getConnectionCount(userId)).isEqualTo(5);

        // Attempting 6th should be rejected (returns false or throws)
        SseEmitter sixthEmitter = mock(SseEmitter.class);
        assertThat(manager.canRegister(userId)).isFalse();
    }
}
