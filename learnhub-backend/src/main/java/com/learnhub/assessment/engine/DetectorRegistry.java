package com.learnhub.assessment.engine;

import java.util.List;
import java.util.Map;

/**
 * Registry for competency detectors.
 *
 * Manages detector lifecycle, discovery, and execution:
 * - Auto-discovers Spring @Component implementations of CompetencyDetector
 * - Orders detectors by priority (for dependencies and optimization)
 * - Supports enabling/disabling specific detectors
 * - Enforces timeout per detector
 * - Handles detector failures gracefully
 *
 * Example Usage:
 *
 * <pre>{@code
 * List<CompetencyDetector> detectors = registry.getDetectors();
 * // or
 * List<CompetencyDetector> enabledDetectors = registry.getEnabledDetectors();
 * }</pre>
 *
 * Thread-safe for concurrent access from multiple evaluation threads.
 */
public interface DetectorRegistry {

    /**
     * Get all registered detectors.
     *
     * Returns detectors in priority order (high priority first).
     * Includes both enabled and disabled detectors.
     *
     * @return list of all detectors, ordered by priority
     */
    List<CompetencyDetector> getDetectors();

    /**
     * Get enabled detectors.
     *
     * Returns only detectors that have not been explicitly disabled.
     * Useful for selective evaluation (skip certain detectors).
     *
     * Example: Skip slow detectors in CI/CD pipeline
     *
     * @return list of enabled detectors, ordered by priority
     */
    List<CompetencyDetector> getEnabledDetectors();

    /**
     * Get detector by competency name.
     *
     * Useful for per-competency configuration or debugging.
     *
     * @param competencyName the competency identifier (e.g., "api-design")
     * @return the detector, or empty if not found
     */
    CompetencyDetector getDetector(String competencyName);

    /**
     * Check if a detector is enabled.
     *
     * @param competencyName the competency identifier
     * @return true if detector exists and is enabled
     */
    boolean isDetectorEnabled(String competencyName);

    /**
     * Enable a detector.
     *
     * Marks detector as active for subsequent evaluations.
     * No-op if already enabled.
     *
     * @param competencyName the competency identifier
     * @throws IllegalArgumentException if detector not found
     */
    void enableDetector(String competencyName);

    /**
     * Disable a detector.
     *
     * Marks detector as inactive. Disabled detectors are skipped
     * during evaluation (getEnabledDetectors() won't include them).
     *
     * Example: Disable slow detectors for quick evaluations
     *
     * @param competencyName the competency identifier
     * @throws IllegalArgumentException if detector not found
     */
    void disableDetector(String competencyName);

    /**
     * Execute all detectors in parallel for a single repository snapshot.
     *
     * Runs enabled detectors concurrently using a thread pool, collecting
     * results with timeout enforcement per detector.
     *
     * Failures in individual detectors do not block others (graceful degradation).
     *
     * @param snapshot the repository snapshot to analyze
     * @param brfRules the BRF rules for this evaluation
     * @return map of competency name -> detection result
     */
    Map<String, CompetencyDetector.DetectionResult> runDetectorsParallel(
        Object snapshot,
        CompetencyDetector.BrfRules brfRules);

    /**
     * Set timeout per detector (in milliseconds).
     *
     * If a detector takes longer than this timeout to analyze,
     * it may be interrupted or its result discarded.
     *
     * Default: 10000 ms (10 seconds)
     *
     * @param timeoutMs timeout in milliseconds
     * @throws IllegalArgumentException if timeoutMs <= 0
     */
    void setDetectorTimeout(long timeoutMs);

    /**
     * Get timeout per detector (in milliseconds).
     *
     * @return timeout in milliseconds
     */
    long getDetectorTimeout();

    /**
     * Get detector count.
     *
     * @return number of registered detectors
     */
    int getDetectorCount();

    /**
     * Check if any detectors are registered.
     *
     * @return true if at least one detector is registered
     */
    boolean hasDetectors();

    /**
     * Reload/refresh detector registry.
     *
     * Useful after Spring context changes or dynamic detector registration.
     * Safe to call; no-op if no changes.
     */
    void refresh();

    /**
     * Get detector configuration/metadata.
     *
     * Returns detailed info about registered detectors:
     * - Names
     * - Priorities
     * - Enabled/disabled status
     * - Timeout settings
     *
     * Useful for debugging and monitoring.
     *
     * @return detector metadata
     */
    DetectorRegistryInfo getRegistryInfo();

    /**
     * Detector registry metadata.
     *
     * Contains info about all detectors in registry.
     */
    interface DetectorRegistryInfo {
        /**
         * Get all detector names.
         */
        List<String> getDetectorNames();

        /**
         * Get detector priority.
         */
        int getPriority(String competencyName);

        /**
         * Check if detector is enabled.
         */
        boolean isEnabled(String competencyName);

        /**
         * Get total detector count.
         */
        int getTotalDetectors();

        /**
         * Get enabled detector count.
         */
        int getEnabledDetectorCount();
    }
}
