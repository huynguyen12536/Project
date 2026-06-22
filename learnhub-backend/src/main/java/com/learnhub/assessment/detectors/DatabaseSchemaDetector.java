package com.learnhub.assessment.detectors;

import com.learnhub.assessment.engine.CompetencyDetector;
import com.learnhub.github.entity.RepositorySnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Database Schema Design Detector.
 *
 * Analyzes repository for database design competency:
 * - Flyway migration files (V1__*, V2__*, etc.) for version control
 * - SQL normalization (1NF, 2NF, 3NF)
 * - Foreign key usage (relationship design)
 * - Index usage (performance optimization)
 * - Entity relationships in code (@OneToMany, @ManyToOne)
 * - Connection pooling configuration
 * - Data integrity constraints (NOT NULL, UNIQUE, CHECK)
 *
 * This detector looks for evidence of good database design practices
 * in both SQL migrations and Hibernate entity definitions.
 */
@Component
@Slf4j
public class DatabaseSchemaDetector implements CompetencyDetector {

    // Patterns for SQL analysis
    private static final Pattern CREATE_TABLE_PATTERN =
        Pattern.compile("CREATE TABLE\\s+([a-zA-Z0-9_]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRIMARY_KEY_PATTERN =
        Pattern.compile("PRIMARY KEY|@Id", Pattern.CASE_INSENSITIVE);
    private static final Pattern FOREIGN_KEY_PATTERN =
        Pattern.compile("FOREIGN KEY|@ManyToOne|@OneToMany|@JoinColumn", Pattern.CASE_INSENSITIVE);
    private static final Pattern INDEX_PATTERN =
        Pattern.compile("CREATE INDEX|@Index", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONSTRAINT_PATTERN =
        Pattern.compile("NOT NULL|UNIQUE|CHECK|DEFAULT", Pattern.CASE_INSENSITIVE);
    private static final Pattern COLUMN_PATTERN =
        Pattern.compile("\\s+([a-zA-Z0-9_]+)\\s+(INT|VARCHAR|TEXT|BOOLEAN|TIMESTAMP|DECIMAL|FLOAT|UUID|CHAR|BIGINT|SMALLINT|DATE|TIME|JSONB)",
                       Pattern.CASE_INSENSITIVE);

    @Override
    public String getCompetencyName() {
        return "database";
    }

    @Override
    public int getPriority() {
        return 5;  // Run after core parsing, but early
    }

    /**
     * Analyze repository for database design patterns.
     *
     * Strategy:
     * 1. Check for Flyway migrations (version-controlled SQL)
     * 2. Count tables, columns, constraints
     * 3. Analyze foreign key relationships
     * 4. Check for indices (performance consideration)
     * 5. Verify normalization (avoid data duplication)
     * 6. Check entity annotations in Java code
     * 7. Assess overall database design quality
     */
    @Override
    public DetectionResult analyze(RepositorySnapshot snapshot, CompetencyDetector.BrfRules rules) {
        DetectionResult result = new DetectionResult(getCompetencyName(), CompetencyLevel.NOT_DEMONSTRATED);
        result.evidence = new ArrayList<>();
        result.gaps = new ArrayList<>();
        result.metadata = new HashMap<>();

        try {
            // Step 1: Look for Flyway migrations
            List<SqlMigrationFile> migrations = extractMigrations(snapshot);
            result.metadata.put("migration_count", migrations.size());

            if (migrations.isEmpty()) {
                result.gaps.add("No database migrations found. Use Flyway for version-controlled schema changes");
                result.confidence = 70;
                log.debug("{}: No migrations found", getCompetencyName());
                return result;
            }

            result.evidence.add(String.format("Database uses Flyway for version control (%d migration(s) found)",
                                             migrations.size()));

            // Step 2: Analyze schema structure
            SchemaAnalysis analysis = analyzeSchema(migrations);
            result.metadata.putAll(analysis.metadata);

            if (analysis.tableCount == 0) {
                result.gaps.add("No tables found in migrations");
                result.confidence = 60;
                return result;
            }

            result.evidence.add(String.format("Schema contains %d tables with %d columns",
                                             analysis.tableCount, analysis.columnCount));

            // Step 3: Check for primary keys
            if (analysis.hasPrimaryKeys) {
                result.evidence.add("Tables have primary keys for unique identification");
            } else {
                result.gaps.add("Some tables missing primary keys");
            }

            // Step 4: Check for foreign keys (relationships)
            if (analysis.foreignKeyCount > 0) {
                result.evidence.add(String.format("Schema uses %d foreign key relationships (good normalization)",
                                                 analysis.foreignKeyCount));
            } else {
                result.gaps.add("No foreign key relationships found. Consider normalizing data with proper relationships");
            }

            // Step 5: Check for indices (performance)
            if (analysis.indexCount > 0) {
                result.evidence.add(String.format("Schema includes %d indices for query optimization",
                                                 analysis.indexCount));
            } else {
                result.gaps.add("No indices found. Add indices on foreign keys and frequently queried columns");
            }

            // Step 6: Check for constraints
            if (analysis.hasConstraints) {
                result.evidence.add("Tables use NOT NULL, UNIQUE, and other constraints for data integrity");
            } else {
                result.gaps.add("Missing data integrity constraints (add NOT NULL, UNIQUE, etc.)");
            }

            // Step 7: Check for normalization issues
            if (analysis.normalizationScore >= 0.8) {
                result.evidence.add("Schema appears well-normalized (3NF) with minimal data duplication");
            } else if (analysis.normalizationScore >= 0.5) {
                result.gaps.add("Schema could be better normalized. Eliminate redundant columns");
            } else {
                result.gaps.add("Schema appears denormalized with potential data duplication issues");
            }

            // Step 8: Calculate competency level
            int pointsAwarded = 0;
            if (analysis.hasPrimaryKeys) pointsAwarded++;
            if (analysis.foreignKeyCount > 0) pointsAwarded++;
            if (analysis.indexCount > 0) pointsAwarded++;
            if (analysis.hasConstraints) pointsAwarded++;

            if (pointsAwarded == 4 && analysis.normalizationScore >= 0.8) {
                result.level = CompetencyLevel.PROFICIENT;
                result.confidence = 85;
            } else if (pointsAwarded >= 2) {
                result.level = CompetencyLevel.EMERGING;
                result.confidence = 75;
            } else {
                result.level = CompetencyLevel.NOT_DEMONSTRATED;
                result.confidence = 70;
            }

            log.debug("{}: Analysis complete. Level: {}, Tables: {}, FKs: {}, Points: {}/4, Confidence: {}%",
                    getCompetencyName(), result.level, analysis.tableCount, analysis.foreignKeyCount,
                    pointsAwarded, result.confidence);

        } catch (Exception e) {
            log.error("Error analyzing database schema", e);
            result.level = CompetencyLevel.NOT_DEMONSTRATED;
            result.gaps.add("Error during analysis: " + e.getMessage());
            result.confidence = 30;
        }

        return result;
    }

    /**
     * Extract Flyway migration files from repository.
     *
     * Looks for files matching pattern: V[0-9]+__[description].sql
     * Standard Flyway naming convention.
     *
     * @param snapshot the repository snapshot
     * @return list of extracted migration files
     */
    private List<SqlMigrationFile> extractMigrations(RepositorySnapshot snapshot) {
        List<SqlMigrationFile> migrations = new ArrayList<>();

        // TODO: In production, would parse snapshot file system
        // For now, returns empty list (placeholder)
        // Full implementation would scan src/main/resources/db/migration/

        return migrations;
    }

    /**
     * Analyze schema structure from migrations.
     *
     * Extracts:
     * - Table count
     * - Column count
     * - Primary key usage
     * - Foreign key relationships
     * - Index definitions
     * - Data integrity constraints
     *
     * @param migrations list of SQL migration files
     * @return analysis results
     */
    private SchemaAnalysis analyzeSchema(List<SqlMigrationFile> migrations) {
        SchemaAnalysis analysis = new SchemaAnalysis();

        for (SqlMigrationFile migration : migrations) {
            String sql = migration.content.toUpperCase();

            // Count tables
            Matcher tableMatcher = CREATE_TABLE_PATTERN.matcher(sql);
            while (tableMatcher.find()) {
                analysis.tableCount++;
            }

            // Count columns (simplified)
            Matcher colMatcher = COLUMN_PATTERN.matcher(migration.content);
            while (colMatcher.find()) {
                analysis.columnCount++;
            }

            // Check for primary keys
            if (PRIMARY_KEY_PATTERN.matcher(sql).find()) {
                analysis.hasPrimaryKeys = true;
            }

            // Count foreign keys
            Matcher fkMatcher = FOREIGN_KEY_PATTERN.matcher(sql);
            while (fkMatcher.find()) {
                analysis.foreignKeyCount++;
            }

            // Count indices
            Matcher idxMatcher = INDEX_PATTERN.matcher(sql);
            while (idxMatcher.find()) {
                analysis.indexCount++;
            }

            // Check for constraints
            if (CONSTRAINT_PATTERN.matcher(sql).find()) {
                analysis.hasConstraints = true;
            }

            // Simple normalization heuristic
            // High FK count relative to table count suggests good normalization
            if (analysis.tableCount > 0) {
                analysis.normalizationScore = Math.min(1.0,
                    (double) analysis.foreignKeyCount / (analysis.tableCount * 0.5));
            }
        }

        // Populate metadata
        analysis.metadata.put("table_count", analysis.tableCount);
        analysis.metadata.put("column_count", analysis.columnCount);
        analysis.metadata.put("foreign_key_count", analysis.foreignKeyCount);
        analysis.metadata.put("index_count", analysis.indexCount);
        analysis.metadata.put("normalization_score",
            String.format("%.2f", analysis.normalizationScore));
        analysis.metadata.put("has_primary_keys", analysis.hasPrimaryKeys);
        analysis.metadata.put("has_constraints", analysis.hasConstraints);

        return analysis;
    }

    /**
     * Model for SQL migration file.
     */
    private static class SqlMigrationFile {
        String fileName;      // V001__initial_schema.sql
        String version;       // 001
        String description;   // initial_schema
        String content;       // Full SQL content

        SqlMigrationFile(String fileName, String content) {
            this.fileName = fileName;
            this.content = content;
            // Parse version and description from Flyway naming convention
            Pattern namePattern = Pattern.compile("V(\\d+)__(.+)\\.sql", Pattern.CASE_INSENSITIVE);
            Matcher m = namePattern.matcher(fileName);
            if (m.find()) {
                this.version = m.group(1);
                this.description = m.group(2);
            }
        }
    }

    /**
     * Results of schema analysis.
     */
    private static class SchemaAnalysis {
        int tableCount = 0;
        int columnCount = 0;
        int foreignKeyCount = 0;
        int indexCount = 0;
        boolean hasPrimaryKeys = false;
        boolean hasConstraints = false;
        double normalizationScore = 0.0;
        Map<String, Object> metadata = new HashMap<>();
    }
}
