package com.learnhub.brf.service;

import com.learnhub.brf.entity.BrfVersion;
import com.learnhub.brf.exception.BrfVersionNotFoundException;
import com.learnhub.brf.repository.BrfVersionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of BrfService with caching and version management.
 *
 * Handles loading and caching BRF YAML rules from database.
 * Uses Spring @Cacheable for transparent in-memory caching with 24-hour TTL.
 *
 * Cache Configuration (application.yml):
 * spring.cache.caffeine.spec: maximumSize=100,expireAfterWrite=24h
 */
@Service
@Transactional
@Slf4j
public class BrfServiceImpl implements BrfService {

    private final BrfVersionRepository brfVersionRepository;

    @Value("${brf.default-version:1.0.0}")
    private String defaultVersion;

    /**
     * Create BRF service.
     *
     * @param brfVersionRepository repository for BRF version queries
     */
    public BrfServiceImpl(BrfVersionRepository brfVersionRepository) {
        this.brfVersionRepository = brfVersionRepository;
        log.info("BrfServiceImpl initialized");
    }

    /**
     * Get BRF version metadata.
     *
     * Queries database for version information.
     * Throws BrfVersionNotFoundException if version not found.
     *
     * @param version semantic version string
     * @return BrfVersion entity with all metadata
     * @throws BrfVersionNotFoundException if version not found
     */
    @Override
    @Transactional(readOnly = true)
    public BrfVersion getVersion(String version) {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("BRF version must not be null or blank");
        }

        return brfVersionRepository.findByVersion(version)
                .orElseThrow(() -> {
                    log.warn("BRF version not found: {}", version);
                    return new BrfVersionNotFoundException(
                            "BRF version not found: " + version);
                });
    }

    /**
     * Load YAML rules for a BRF version.
     *
     * Caches YAML content in memory with 24-hour TTL.
     * Subsequent calls within TTL return cached content.
     * Cache key: "brfYaml::<version>"
     *
     * @param version semantic version string
     * @return YAML content as string
     * @throws BrfVersionNotFoundException if version not found
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "brfYaml", key = "#version")
    public String loadYaml(String version) {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("BRF version must not be null or blank");
        }

        BrfVersion brfVersion = getVersion(version);
        log.debug("Loaded BRF YAML from cache/database: version={}", version);
        return brfVersion.getYamlContent();
    }

    /**
     * Check if BRF version exists.
     *
     * @param version semantic version string
     * @return true if version exists, false otherwise
     */
    @Override
    @Transactional(readOnly = true)
    public boolean versionExists(String version) {
        if (version == null || version.isBlank()) {
            return false;
        }
        return brfVersionRepository.existsByVersion(version);
    }

    /**
     * Create or update a BRF version.
     *
     * Validates inputs and persists to database.
     * If version already exists, updates metadata and YAML content.
     *
     * @param version semantic version string
     * @param yamlContent YAML rules as string
     * @param gitTag optional Git tag
     * @param commitHash optional Git commit hash
     * @return saved BrfVersion entity
     * @throws IllegalArgumentException if required parameters are invalid
     */
    @Override
    public BrfVersion saveVersion(
            String version,
            String yamlContent,
            String gitTag,
            String commitHash) {

        // Validate inputs
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("BRF version must not be null or blank");
        }
        if (yamlContent == null || yamlContent.isBlank()) {
            throw new IllegalArgumentException("YAML content must not be null or blank");
        }
        if (!version.matches("^\\d+\\.\\d+\\.\\d+(-[a-zA-Z0-9.]+)?$")) {
            throw new IllegalArgumentException(
                    "Version must be valid semantic version (e.g., 1.0.0): " + version);
        }

        // Check if version already exists
        BrfVersion brfVersion = brfVersionRepository.findByVersion(version)
                .orElse(new BrfVersion());

        brfVersion.setVersion(version);
        brfVersion.setYamlContent(yamlContent);
        brfVersion.setGitTag(gitTag);
        brfVersion.setCommitHash(commitHash);

        BrfVersion saved = brfVersionRepository.save(brfVersion);
        log.info("BRF version saved: version={}, gitTag={}",
                version, gitTag != null ? gitTag : "none");

        return saved;
    }

    /**
     * Clear YAML cache.
     *
     * Forces reload from database on next loadYaml() call.
     * Use after updates or to free memory.
     */
    @Override
    @CacheEvict(value = "brfYaml", allEntries = true)
    public void clearCache() {
        log.info("BRF YAML cache cleared");
    }

    /**
     * Get default BRF version from configuration.
     *
     * @return default version string from brf.default-version property
     */
    @Override
    public String getDefaultVersion() {
        return defaultVersion;
    }
}
