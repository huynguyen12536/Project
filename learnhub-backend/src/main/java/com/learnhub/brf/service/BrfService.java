package com.learnhub.brf.service;

import com.learnhub.brf.entity.BrfVersion;

/**
 * Service interface for BRF (Behavior and Rule Framework) version management.
 *
 * Provides abstraction for loading and managing BRF rule versions.
 * Supports multiple versioning strategies:
 * - Database versioning: YAML stored in PostgreSQL
 * - Git versioning: Tags mapped to commit hashes
 * - Artifact storage: S3/MinIO for large YAML files
 *
 * All YAML content is cached in-memory with 24-hour TTL.
 */
public interface BrfService {

    /**
     * Get metadata for a specific BRF version.
     *
     * Queries database for version information including Git tags,
     * artifact URLs, and creation timestamps. Does not load YAML content.
     *
     * @param version semantic version string (e.g., "1.0.0")
     * @return BrfVersion entity with metadata
     * @throws com.learnhub.brf.exception.BrfVersionNotFoundException if version not found
     */
    BrfVersion getVersion(String version);

    /**
     * Load YAML rules for a specific BRF version.
     *
     * Returns parsed YAML content as string. Content is cached in-memory
     * with 24-hour TTL to avoid repeated database queries.
     *
     * Falls back to local cache if artifact download fails.
     *
     * @param version semantic version string
     * @return YAML rules content as string
     * @throws com.learnhub.brf.exception.BrfVersionNotFoundException if version not found
     * @throws RuntimeException if YAML loading fails (after fallback attempts)
     */
    String loadYaml(String version);

    /**
     * Check if a BRF version exists.
     *
     * @param version semantic version string
     * @return true if version exists in database, false otherwise
     */
    boolean versionExists(String version);

    /**
     * Create or update a BRF version.
     *
     * Stores YAML content in database and optionally uploads to artifact storage.
     * If version already exists, updates metadata and YAML content.
     *
     * @param version semantic version string
     * @param yamlContent YAML rules as string
     * @param gitTag optional Git tag (e.g., "v1.0.0")
     * @param commitHash optional Git commit hash
     * @return saved BrfVersion entity
     * @throws IllegalArgumentException if version, yamlContent, or gitTag is invalid
     */
    BrfVersion saveVersion(
            String version,
            String yamlContent,
            String gitTag,
            String commitHash);

    /**
     * Clear in-memory YAML cache.
     *
     * Forces reload of YAML from database on next call.
     * Useful after updates or to free memory.
     */
    void clearCache();

    /**
     * Get default BRF version from application configuration.
     *
     * Reads brf.default-version from application.yml.
     * Used when assessment doesn't specify a version.
     *
     * @return default semantic version string
     */
    String getDefaultVersion();
}
