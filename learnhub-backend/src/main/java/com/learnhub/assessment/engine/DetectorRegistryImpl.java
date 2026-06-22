package com.learnhub.assessment.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Spring implementation of DetectorRegistry.
 *
 * Auto-discovers all @Component implementations of CompetencyDetector
 * and manages their lifecycle:
 * - Auto-ordering by priority
 * - Enable/disable support
 * - Timeout enforcement
 * - Graceful failure handling
 *
 * Thread-safe for concurrent access from multiple evaluation threads.
 */
@Component
@Slf4j
public class DetectorRegistryImpl implements DetectorRegistry {

    /**
     * All registered detectors (auto-discovered by Spring).
     * Immutable snapshot; won't change after construction.
     */
    private final List<CompetencyDetector> allDetectors;

    /**
     * Enabled/disabled status per detector (mutable).
     * Maps competency name -> enabled flag.
     */
    private final Map<String, Boolean> enabledStatus;

    /**
     * Quick lookup map: competency name -> detector.
     */
    private final Map<String, CompetencyDetector> detectorsByName;

    /**
     * Timeout per detector in milliseconds.
     * Can be changed at runtime.
     */
    private volatile long detectorTimeoutMs = 10000;  // Default: 10 seconds

    /**
     * Thread pool for parallel detector execution.
     * Configurable pool size for parallelism control.
     */
    private final ExecutorService executorService;

    @Value("${assessment.detector-thread-pool-size:4}")
    private int threadPoolSize = 4;

    /**
     * Create registry and discover detectors.
     *
     * @param detectors collection of detectors auto-wired by Spring
     *                  (via Collection<CompetencyDetector> injection)
     */
    public DetectorRegistryImpl(Collection<CompetencyDetector> detectors) {
        // Sort by priority (descending)
        this.allDetectors = detectors.stream()
            .sorted(Comparator.comparingInt(CompetencyDetector::getPriority).reversed())
            .collect(Collectors.toList());

        // Initialize enable/disable status (all enabled by default)
        this.enabledStatus = new ConcurrentHashMap<>();
        this.allDetectors.forEach(d ->
            enabledStatus.put(d.getCompetencyName(), true)
        );

        // Build lookup map
        this.detectorsByName = new ConcurrentHashMap<>();
        this.allDetectors.forEach(d ->
            detectorsByName.put(d.getCompetencyName(), d)
        );

        // Initialize thread pool for parallel execution
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);

        logRegistry();
    }

    @Override
    public List<CompetencyDetector> getDetectors() {
        return new ArrayList<>(allDetectors);
    }

    @Override
    public List<CompetencyDetector> getEnabledDetectors() {
        return allDetectors.stream()
            .filter(d -> isDetectorEnabled(d.getCompetencyName()))
            .collect(Collectors.toList());
    }

    @Override
    public CompetencyDetector getDetector(String competencyName) {
        return detectorsByName.get(competencyName);
    }

    @Override
    public boolean isDetectorEnabled(String competencyName) {
        return enabledStatus.getOrDefault(competencyName, false);
    }

    @Override
    public void enableDetector(String competencyName) {
        if (!detectorsByName.containsKey(competencyName)) {
            throw new IllegalArgumentException(
                "Detector not found: " + competencyName);
        }
        if (!enabledStatus.getOrDefault(competencyName, false)) {
            enabledStatus.put(competencyName, true);
            log.info("Detector enabled: {}", competencyName);
        }
    }

    @Override
    public void disableDetector(String competencyName) {
        if (!detectorsByName.containsKey(competencyName)) {
            throw new IllegalArgumentException(
                "Detector not found: " + competencyName);
        }
        if (enabledStatus.getOrDefault(competencyName, false)) {
            enabledStatus.put(competencyName, false);
            log.info("Detector disabled: {}", competencyName);
        }
    }

    @Override
    public void setDetectorTimeout(long timeoutMs) {
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException(
                "Timeout must be positive: " + timeoutMs);
        }
        this.detectorTimeoutMs = timeoutMs;
        log.debug("Detector timeout set to {}ms", timeoutMs);
    }

    @Override
    public long getDetectorTimeout() {
        return detectorTimeoutMs;
    }

    @Override
    public int getDetectorCount() {
        return allDetectors.size();
    }

    @Override
    public boolean hasDetectors() {
        return !allDetectors.isEmpty();
    }

    @Override
    public Map<String, CompetencyDetector.DetectionResult> runDetectorsParallel(
            Object snapshot,
            CompetencyDetector.BrfRules brfRules) {
        log.debug("Running {} enabled detectors in parallel", getEnabledDetectors().size());

        List<CompetencyDetector> enabledDetectors = getEnabledDetectors();
        Map<String, CompetencyDetector.DetectionResult> results = new ConcurrentHashMap<>();
        List<Future<Void>> futures = new ArrayList<>();

        // Submit all detector tasks to thread pool
        for (CompetencyDetector detector : enabledDetectors) {
            Future<Void> future = executorService.submit(() -> {
                String competencyName = detector.getCompetencyName();
                try {
                    log.debug("Detector executing: {}", competencyName);
                    CompetencyDetector.DetectionResult result =
                        detector.analyze(snapshot, brfRules);
                    results.put(competencyName, result);
                    log.debug("Detector completed: {} (confidence={})",
                        competencyName, result.confidence);
                } catch (Exception e) {
                    log.warn("Detector failed: {}", competencyName, e);
                    // Create failed result to maintain map consistency
                    CompetencyDetector.DetectionResult failedResult =
                        createFailedDetectionResult(competencyName, e);
                    results.put(competencyName, failedResult);
                }
                return null;
            });
            futures.add(future);
        }

        // Wait for all detectors to complete with timeout
        for (Future<Void> future : futures) {
            try {
                future.get(detectorTimeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                log.warn("Detector timed out after {}ms", detectorTimeoutMs);
                future.cancel(true);
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error waiting for detector result", e);
            }
        }

        log.debug("Parallel detector execution completed with {} results", results.size());
        return results;
    }

    /**
     * Create a failed detection result when detector throws exception.
     */
    private CompetencyDetector.DetectionResult createFailedDetectionResult(
            String competencyName, Exception e) {
        return new CompetencyDetector.DetectionResult(
            competencyName,
            CompetencyDetector.CompetencyLevel.NOT_DEMONSTRATED,
            Collections.singletonList("Detector execution failed: " + e.getMessage()),
            Collections.singletonList("Detector error prevented analysis"),
            0,  // confidence = 0
            Collections.emptyMap()
        );
    }

    @Override
    public void refresh() {
        log.debug("Detector registry refresh requested");
        // In current implementation, detectors are immutable after construction
        // This is a placeholder for future dynamic detector registration
    }

    @Override
    public DetectorRegistryInfo getRegistryInfo() {
        return new DetectorRegistryInfoImpl();
    }

    /**
     * Log detected detectors on startup.
     */
    private void logRegistry() {
        if (allDetectors.isEmpty()) {
            log.warn("No CompetencyDetector implementations found");
            return;
        }

        log.info("Detector registry initialized with {} detector(s):",
            allDetectors.size());
        for (CompetencyDetector d : allDetectors) {
            log.info("  - {} (priority={})",
                d.getCompetencyName(), d.getPriority());
        }
    }

    /**
     * Implementation of DetectorRegistryInfo.
     *
     * Provides snapshot of registry state at query time.
     */
    private class DetectorRegistryInfoImpl implements DetectorRegistryInfo {

        @Override
        public List<String> getDetectorNames() {
            return allDetectors.stream()
                .map(CompetencyDetector::getCompetencyName)
                .collect(Collectors.toList());
        }

        @Override
        public int getPriority(String competencyName) {
            CompetencyDetector detector = detectorsByName.get(competencyName);
            if (detector == null) {
                throw new IllegalArgumentException(
                    "Detector not found: " + competencyName);
            }
            return detector.getPriority();
        }

        @Override
        public boolean isEnabled(String competencyName) {
            return isDetectorEnabled(competencyName);
        }

        @Override
        public int getTotalDetectors() {
            return allDetectors.size();
        }

        @Override
        public int getEnabledDetectorCount() {
            return Math.toIntExact(
                allDetectors.stream()
                    .filter(d -> isDetectorEnabled(d.getCompetencyName()))
                    .count()
            );
        }
    }
}
