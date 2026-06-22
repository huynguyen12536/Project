package com.learnhub.assessment.analysis;

import com.learnhub.github.entity.RepositorySnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class StaticAnalysisEngine {

    public StaticAnalysisResult analyze(RepositorySnapshot snapshot) {
        StaticAnalysisResult result = new StaticAnalysisResult();

        // Rule Set 1: Security Patterns
        findSecurityIssues(snapshot, result);

        // Rule Set 2: Database Patterns
        findDatabaseIssues(snapshot, result);

        // Rule Set 3: Architecture Patterns
        findArchitectureIssues(snapshot, result);

        // Rule Set 4: Code Quality Patterns
        findCodeQualityIssues(snapshot, result);

        log.debug("Static analysis complete: {} findings", result.getFindings().size());
        return result;
    }

    private void findSecurityIssues(RepositorySnapshot snapshot, StaticAnalysisResult result) {
        String content = snapshot.getFilesContent();

        // Pattern 1: SQL Injection (string concatenation in queries)
        if (content.contains("\"SELECT") || content.contains("\"DELETE") || content.contains("\"UPDATE")) {
            if (content.contains("+ \"") || content.contains("\" + ")) {
                result.addFinding("Security", "Potential SQL injection: raw string concatenation detected",
                    "HIGH", "Database queries");
            }
        }

        // Pattern 2: Hardcoded secrets
        if (content.contains("password=") || content.contains("api_key=") ||
            content.contains("secret=") || content.contains("token=")) {
            result.addFinding("Security", "Hardcoded credentials detected in source",
                "CRITICAL", "Configuration files");
        }

        // Pattern 3: Missing authentication checks
        if (content.contains("@RestController") && content.contains("@GetMapping") &&
            !content.contains("@PreAuthorize") && !content.contains("@Secured")) {
            result.addFinding("Security", "REST endpoints may lack authentication checks",
                "HIGH", "REST controllers");
        }
    }

    private void findDatabaseIssues(RepositorySnapshot snapshot, StaticAnalysisResult result) {
        String content = snapshot.getFilesContent();

        // Pattern 1: Unclosed connections
        if (content.contains("Connection conn =") || content.contains("getConnection()")) {
            if (!content.contains("conn.close()") || !content.contains("try-with-resources")) {
                result.addFinding("Database", "Connection may not be properly closed - potential resource leak",
                    "HIGH", "Database layer");
            }
        }

        // Pattern 2: Unoptimized findAll queries
        if (content.contains("repository.findAll()") && !content.contains("@Query")) {
            result.addFinding("Database", "Unoptimized query: findAll() may load unnecessary columns",
                "MEDIUM", "Repository layer");
        }

        // Pattern 3: Missing indexes on foreign keys
        if (content.contains("@ManyToOne") || content.contains("@OneToMany")) {
            if (!content.contains("@Index")) {
                result.addFinding("Database", "Foreign key relationship lacks index definition",
                    "MEDIUM", "Entity mapping");
            }
        }

        // Pattern 4: N+1 query potential
        if (content.contains("for (") && content.contains("entity.get")) {
            result.addFinding("Database", "Potential N+1 query: loop with entity access detected",
                "MEDIUM", "Service layer");
        }
    }

    private void findArchitectureIssues(RepositorySnapshot snapshot, StaticAnalysisResult result) {
        String content = snapshot.getFilesContent();

        // Pattern 1: Returning entities from REST endpoints (missing DTOs)
        if (content.contains("@RestController") && content.contains("@GetMapping")) {
            if (content.contains("return ") && (content.contains("Entity") || content.contains("Model"))) {
                result.addFinding("Architecture", "Returning entities from REST endpoints - use DTOs for decoupling",
                    "MEDIUM", "REST controller layer");
            }
        }

        // Pattern 2: Missing service layer (direct repository access in controller)
        if (content.contains("@RestController") && content.contains("@Autowired") &&
            content.contains("Repository")) {
            result.addFinding("Architecture", "Controller directly accesses repository - use service layer abstraction",
                "MEDIUM", "Layering");
        }

        // Pattern 3: God service classes (> 500 lines)
        if (snapshot.getTopFiles().stream()
            .filter(f -> f.getName().endsWith("Service.java"))
            .anyMatch(f -> f.getLineCount() > 500)) {
            result.addFinding("Architecture", "Service class is too large - consider splitting by responsibility",
                "MEDIUM", "Service layer organization");
        }

        // Pattern 4: Mixed concerns in single class
        if (content.contains("@Service") && content.contains("@Repository")) {
            result.addFinding("Architecture", "Service class also implements repository pattern - violates separation of concerns",
                "HIGH", "Class responsibility");
        }
    }

    private void findCodeQualityIssues(RepositorySnapshot snapshot, StaticAnalysisResult result) {
        String content = snapshot.getFilesContent();

        // Pattern 1: Unsafe Optional access
        int unsafeOptionals = countOccurrences(content, ".get()") - countOccurrences(content, ".orElse");
        if (unsafeOptionals > 5) {
            result.addFinding("Code Quality", "Missing null safety: unsafe Optional.get() detected " + unsafeOptionals + " times",
                "MEDIUM", "Throughout codebase");
        }

        // Pattern 2: Missing try-catch handlers
        int tryBlocks = countOccurrences(content, "try {");
        int catchBlocks = countOccurrences(content, "catch (");
        if (tryBlocks > catchBlocks + 2) {
            result.addFinding("Code Quality", "Try blocks without matching catch handlers found",
                "LOW", "Exception handling");
        }

        // Pattern 3: No logging
        if (!content.contains("log.") && !content.contains("logger.")) {
            result.addFinding("Code Quality", "No logging detected - consider adding observability",
                "LOW", "Logging strategy");
        }

        // Pattern 4: Magic numbers
        if (content.matches(".*\\b[0-9]{3,}\\b.*")) {
            result.addFinding("Code Quality", "Magic numbers detected - extract to named constants",
                "LOW", "Maintainability");
        }
    }

    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }
}
