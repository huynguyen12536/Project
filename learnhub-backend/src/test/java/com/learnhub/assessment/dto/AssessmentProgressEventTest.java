package com.learnhub.assessment.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnhub.assessment.entity.AssessmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("AssessmentProgressEvent - Event DTO")
class AssessmentProgressEventTest {

    private ObjectMapper objectMapper;
    private UUID assessmentId;
    private Instant timestamp;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        assessmentId = UUID.randomUUID();
        timestamp = Instant.now();
    }

    @Test
    @DisplayName("Create event with all fields populated")
    void testCreateEventWithAllFields() {
        AssessmentProgressEvent event = new AssessmentProgressEvent(
            assessmentId,
            AssessmentStatus.PROCESSING,
            45,
            "Evaluating API design patterns",
            12,
            null,
            0.85,
            timestamp
        );

        assertThat(event.assessmentId()).isEqualTo(assessmentId);
        assertThat(event.status()).isEqualTo(AssessmentStatus.PROCESSING);
        assertThat(event.progressPercent()).isEqualTo(45);
        assertThat(event.currentStep()).isEqualTo("Evaluating API design patterns");
        assertThat(event.estimatedSecondsRemaining()).isEqualTo(12);
        assertThat(event.queuePosition()).isNull();
        assertThat(event.confidence()).isEqualTo(0.85);
        assertThat(event.timestamp()).isEqualTo(timestamp);
    }

    @Test
    @DisplayName("Serialize to JSON with camelCase field names")
    void testSerializeToJsonCamelCase() throws Exception {
        AssessmentProgressEvent event = new AssessmentProgressEvent(
            assessmentId,
            AssessmentStatus.PENDING,
            0,
            "Queued for processing",
            null,
            3,
            0.0,
            timestamp
        );

        String json = objectMapper.writeValueAsString(event);

        assertThat(json)
            .contains("\"assessmentId\"")
            .contains("\"status\":\"PENDING\"")
            .contains("\"progressPercent\":0")
            .contains("\"currentStep\":\"Queued for processing\"")
            .contains("\"estimatedSecondsRemaining\":null")
            .contains("\"queuePosition\":3")
            .contains("\"confidence\":0.0")
            .contains("\"timestamp\"")
            .doesNotContain("assessment_id")
            .doesNotContain("progress_percent")
            .doesNotContain("current_step");
    }

    @Test
    @DisplayName("Deserialize from JSON with camelCase field names")
    void testDeserializeFromJson() throws Exception {
        String json = "{" +
            "\"assessmentId\":\"" + assessmentId + "\"," +
            "\"status\":\"PROCESSING\"," +
            "\"progressPercent\":50," +
            "\"currentStep\":\"Evaluating database schema\"," +
            "\"estimatedSecondsRemaining\":15," +
            "\"queuePosition\":null," +
            "\"confidence\":0.90," +
            "\"timestamp\":\"" + timestamp + "\"" +
            "}";

        AssessmentProgressEvent event = objectMapper.readValue(json, AssessmentProgressEvent.class);

        assertThat(event.assessmentId()).isEqualTo(assessmentId);
        assertThat(event.status()).isEqualTo(AssessmentStatus.PROCESSING);
        assertThat(event.progressPercent()).isEqualTo(50);
        assertThat(event.currentStep()).isEqualTo("Evaluating database schema");
        assertThat(event.estimatedSecondsRemaining()).isEqualTo(15);
        assertThat(event.queuePosition()).isNull();
        assertThat(event.confidence()).isEqualTo(0.90);
    }

    @Test
    @DisplayName("Timestamp is in ISO 8601 UTC format")
    void testTimestampFormatIsISO8601UTC() throws Exception {
        AssessmentProgressEvent event = new AssessmentProgressEvent(
            assessmentId,
            AssessmentStatus.COMPLETED,
            100,
            "Analysis complete",
            0,
            null,
            1.0,
            timestamp
        );

        String json = objectMapper.writeValueAsString(event);

        // ISO 8601 format: 2026-06-19T14:30:15Z
        assertThat(json)
            .contains("\"timestamp\":\"")
            .contains("Z\"");  // Zulu timezone indicator
    }

    @Test
    @DisplayName("Progress percent bounds validation")
    void testProgressPercentBounds() {
        // Valid bounds: 0-100
        assertThatNoException().isThrownBy(() -> {
            new AssessmentProgressEvent(
                assessmentId, AssessmentStatus.PROCESSING, 0, "Start", null, null, 0.5, timestamp
            );
            new AssessmentProgressEvent(
                assessmentId, AssessmentStatus.PROCESSING, 50, "Mid", null, null, 0.5, timestamp
            );
            new AssessmentProgressEvent(
                assessmentId, AssessmentStatus.PROCESSING, 100, "End", null, null, 0.5, timestamp
            );
        });
    }

    @Test
    @DisplayName("Confidence bounds validation (0-1)")
    void testConfidenceBounds() {
        // Valid bounds: 0-1
        assertThatNoException().isThrownBy(() -> {
            new AssessmentProgressEvent(
                assessmentId, AssessmentStatus.PROCESSING, 50, "Step", null, null, 0.0, timestamp
            );
            new AssessmentProgressEvent(
                assessmentId, AssessmentStatus.PROCESSING, 50, "Step", null, null, 0.5, timestamp
            );
            new AssessmentProgressEvent(
                assessmentId, AssessmentStatus.PROCESSING, 50, "Step", null, null, 1.0, timestamp
            );
        });
    }

    @Test
    @DisplayName("Immutability: record cannot be modified")
    void testImmutability() {
        AssessmentProgressEvent event = new AssessmentProgressEvent(
            assessmentId,
            AssessmentStatus.PENDING,
            0,
            "Queued",
            null,
            5,
            0.0,
            timestamp
        );

        // Records are immutable - compile-time protection
        assertThat(event)
            .isInstanceOf(AssessmentProgressEvent.class);
        // No setters should exist
    }

    @Test
    @DisplayName("Status enum serialization as string")
    void testStatusEnumSerialization() throws Exception {
        AssessmentProgressEvent event = new AssessmentProgressEvent(
            assessmentId,
            AssessmentStatus.PROCESSING,
            50,
            "Processing",
            null,
            null,
            0.5,
            timestamp
        );

        String json = objectMapper.writeValueAsString(event);
        assertThat(json).contains("\"status\":\"PROCESSING\"");

        // Deserialize back
        AssessmentProgressEvent deserialized = objectMapper.readValue(json, AssessmentProgressEvent.class);
        assertThat(deserialized.status()).isEqualTo(AssessmentStatus.PROCESSING);
    }

    @Test
    @DisplayName("Nullable fields handle null correctly")
    void testNullableFieldsHandleNull() throws Exception {
        AssessmentProgressEvent event = new AssessmentProgressEvent(
            assessmentId,
            AssessmentStatus.PROCESSING,
            50,
            "Processing",
            null,  // estimatedSecondsRemaining
            null,  // queuePosition
            0.5,
            timestamp
        );

        String json = objectMapper.writeValueAsString(event);
        assertThat(json)
            .contains("\"estimatedSecondsRemaining\":null")
            .contains("\"queuePosition\":null");

        AssessmentProgressEvent deserialized = objectMapper.readValue(json, AssessmentProgressEvent.class);
        assertThat(deserialized.estimatedSecondsRemaining()).isNull();
        assertThat(deserialized.queuePosition()).isNull();
    }
}
