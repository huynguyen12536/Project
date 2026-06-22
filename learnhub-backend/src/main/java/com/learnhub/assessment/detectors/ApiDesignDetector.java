package com.learnhub.assessment.detectors;

import com.learnhub.assessment.engine.CompetencyDetector;
import com.learnhub.github.entity.RepositorySnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * REST API Design Detector.
 *
 * Analyzes repository for REST API design competency:
 * - Endpoint naming conventions (resource-based: /users, /posts)
 * - HTTP method usage (GET for retrieval, POST for creation)
 * - Status code returns (200, 201, 400, 404, 500)
 * - Error handling patterns
 * - Request/response consistency
 *
 * This is an example detector implementation.
 * Duplicates this pattern for other detectors: DatabaseDetector, AuthDetector, etc.
 */
@Component
@Slf4j
public class ApiDesignDetector implements CompetencyDetector {

    @Override
    public String getCompetencyName() {
        return "api-design";
    }

    @Override
    public int getPriority() {
        return 10;  // Run early; other detectors may depend on this
    }

    /**
     * Analyze repository for REST API design patterns.
     *
     * Strategy:
     * 1. Extract Java files (controllers, endpoints)
     * 2. Scan for @RestController, @RequestMapping annotations
     * 3. Check endpoint naming (resource-based)
     * 4. Check HTTP methods (GET, POST, PUT, DELETE usage)
     * 5. Check status codes (200, 201, 400, 404, 500 returns)
     * 6. Collect evidence of good patterns
     * 7. Identify gaps in implementation
     */
    @Override
    public DetectionResult analyze(RepositorySnapshot snapshot, CompetencyDetector.BrfRules rules) {
        DetectionResult result = new DetectionResult(getCompetencyName(), CompetencyDetector.CompetencyLevel.NOT_DEMONSTRATED);
        result.evidence = new ArrayList<>();
        result.gaps = new ArrayList<>();
        result.metadata = new HashMap<>();

        try {
            // Step 1: Extract API-related files from snapshot
            List<ApiEndpoint> endpoints = extractApiEndpoints(snapshot);
            result.metadata.put("endpoint_count", endpoints.size());

            if (endpoints.isEmpty()) {
                result.gaps.add("No REST API endpoints found in repository");
                result.confidence = 80;  // High confidence in absence
                log.debug("{}: No API endpoints found", getCompetencyName());
                return result;
            }

            // Step 2: Analyze naming conventions
            boolean hasResourceNaming = endpoints.stream()
                    .allMatch(e -> isResourceBased(e.path));

            if (hasResourceNaming) {
                result.evidence.add("All endpoints use resource-based naming conventions (e.g., /users, /posts, /comments)");
            } else {
                result.gaps.add("Some endpoints don't follow resource-based naming (use /users/getAll instead of /users)");
            }

            // Step 3: Analyze HTTP methods
            long getCount = endpoints.stream().filter(e -> "GET".equalsIgnoreCase(e.method)).count();
            long postCount = endpoints.stream().filter(e -> "POST".equalsIgnoreCase(e.method)).count();
            long putCount = endpoints.stream().filter(e -> "PUT".equalsIgnoreCase(e.method)).count();
            long deleteCount = endpoints.stream().filter(e -> "DELETE".equalsIgnoreCase(e.method)).count();

            if (getCount > 0 && postCount > 0 && (putCount > 0 || deleteCount > 0)) {
                result.evidence.add(String.format("HTTP methods used correctly: %d GET (retrieval), %d POST (creation), %d PUT/DELETE (update/delete)",
                        getCount, postCount, putCount + deleteCount));
            } else {
                result.gaps.add("Missing standard HTTP methods (need GET, POST, PUT, DELETE for CRUD operations)");
            }

            // Step 4: Analyze status codes
            boolean hasProperStatusCodes = endpoints.stream()
                    .anyMatch(e -> e.returns200Or201() && e.returns400Or404());

            if (hasProperStatusCodes) {
                result.evidence.add("Endpoints return appropriate status codes (200/201 for success, 400/404 for errors)");
            } else {
                result.gaps.add("Missing proper HTTP status codes (use 200/201 for success, 400/404 for errors, 500 for server errors)");
            }

            // Step 5: Analyze error handling
            boolean hasErrorHandling = endpoints.stream()
                    .anyMatch(e -> e.hasErrorHandling());

            if (hasErrorHandling) {
                result.evidence.add("Error handling implemented with try-catch or exception handlers");
            } else {
                result.gaps.add("Missing error handling on endpoints (add exception handling and error responses)");
            }

            // Step 6: Calculate competency level
            int pointsAwarded = 0;
            if (hasResourceNaming) pointsAwarded++;
            if (getCount > 0 && postCount > 0) pointsAwarded++;
            if (hasProperStatusCodes) pointsAwarded++;
            if (hasErrorHandling) pointsAwarded++;

            if (pointsAwarded == 4) {
                result.level = CompetencyLevel.PROFICIENT;
                result.confidence = 85;
            } else if (pointsAwarded >= 2) {
                result.level = CompetencyLevel.EMERGING;
                result.confidence = 75;
            } else {
                result.level = CompetencyLevel.NOT_DEMONSTRATED;
                result.confidence = 70;
            }

            log.debug("{}: Analysis complete. Level: {}, Points: {}/4, Confidence: {}%",
                    getCompetencyName(), result.level, pointsAwarded, result.confidence);

        } catch (Exception e) {
            log.error("Error analyzing API design", e);
            result.level = CompetencyLevel.NOT_DEMONSTRATED;
            result.gaps.add("Error during analysis: " + e.getMessage());
            result.confidence = 30;  // Low confidence on error
        }

        return result;
    }

    /**
     * Extract API endpoints from repository source code.
     *
     * Simplified implementation: scans for Java files with @RestController
     * and @RequestMapping annotations, extracts endpoint information.
     *
     * TODO: Implement full parsing logic using AST or regex.
     */
    private List<ApiEndpoint> extractApiEndpoints(RepositorySnapshot snapshot) {
        List<ApiEndpoint> endpoints = new ArrayList<>();

        // TODO: Scan repository for Java files
        // TODO: Find files with @RestController annotation
        // TODO: Parse @RequestMapping, @GetMapping, @PostMapping, etc.
        // TODO: Extract path, method, return type, exception handling

        // For now, return empty list (placeholder)
        // Full implementation would require Java AST parsing

        return endpoints;
    }

    /**
     * Check if endpoint path follows resource-based naming.
     *
     * Examples of resource-based:
     * - /users
     * - /users/{id}
     * - /users/{userId}/posts
     * - /api/v1/products
     *
     * Examples of NOT resource-based:
     * - /getUsers
     * - /getUserById/{id}
     * - /users/getAllActive
     */
    private boolean isResourceBased(String path) {
        // Should not contain action verbs (get, create, update, delete)
        Pattern actionVerb = Pattern.compile("/(get|create|update|delete|add|remove|list|fetch|retrieve|search|find)[A-Z]", Pattern.CASE_INSENSITIVE);
        return !actionVerb.matcher(path).find();
    }

    /**
     * Simple model for API endpoint information.
     *
     * In production, would use AST parsing to extract detailed info.
     */
    private static class ApiEndpoint {
        String path;           // "/users", "/posts/{id}", etc.
        String method;         // "GET", "POST", "PUT", "DELETE"
        List<Integer> statusCodes;  // 200, 201, 400, 404, 500, etc.
        boolean hasErrorHandling;

        ApiEndpoint(String path, String method) {
            this.path = path;
            this.method = method;
            this.statusCodes = new ArrayList<>();
            this.hasErrorHandling = false;
        }

        boolean returns200Or201() {
            return statusCodes.contains(200) || statusCodes.contains(201);
        }

        boolean returns400Or404() {
            return statusCodes.contains(400) || statusCodes.contains(404);
        }
    }
}
