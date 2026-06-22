package com.learnhub.assessment.lm;

import com.learnhub.github.entity.RepositorySnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.Map;

@TestComponent
@Profile("test")
@Primary
@Slf4j
public class MockLLMAnalysisService extends LLMAnalysisService {

    private String currentFixture = "PERFECT_REPO";

    public static final Map<String, LLMAnalysisResult> FIXTURES = Map.ofEntries(
        // Fixture 1: Perfectly secure & architected repo
        Map.entry("PERFECT_REPO", new LLMAnalysisResult(
            Map.of(
                "Security", 25,
                "Database", 25,
                "Architecture", 25,
                "Code Quality", 25
            ),
            Map.of(
                "Security", "No security gaps detected. Strong authentication and authorization patterns.",
                "Database", "Database schema well-designed with proper indexing and connection pooling.",
                "Architecture", "Clean layering with well-defined DTOs and service boundaries.",
                "Code Quality", "High readability and comprehensive error handling throughout."
            )
        )),

        // Fixture 2: Repository with security issues
        Map.entry("SECURITY_RISK_REPO", new LLMAnalysisResult(
            Map.of(
                "Security", -15,
                "Database", 5,
                "Architecture", 10,
                "Code Quality", 0
            ),
            Map.of(
                "Security", "Multiple SQL injection vulnerabilities detected. UserRepository.findByEmail() uses string concatenation.",
                "Database", "Query performance is acceptable but lacks indexes on frequently-queried columns.",
                "Architecture", "DTOs present but inconsistently applied. Some endpoints return entities directly.",
                "Code Quality", "Adequate but missing some null safety checks."
            )
        )),

        // Fixture 3: Legacy codebase with architecture issues
        Map.entry("LEGACY_REPO", new LLMAnalysisResult(
            Map.of(
                "Security", 10,
                "Database", -5,
                "Architecture", -20,
                "Code Quality", -10
            ),
            Map.of(
                "Security", "Basic security present but lacks modern patterns like dependency injection.",
                "Database", "N+1 query problems detected. Unoptimized entity mappings.",
                "Architecture", "God service classes with mixed responsibilities. Missing clear separation of concerns.",
                "Code Quality", "High duplication. Error handling inconsistent. Limited test coverage."
            )
        )),

        // Fixture 4: Empty repository (bootstrap scenario)
        Map.entry("EMPTY_REPO", new LLMAnalysisResult(
            Map.of(
                "Security", 0,
                "Database", 0,
                "Architecture", 0,
                "Code Quality", 0
            ),
            Map.of(
                "Security", "No security patterns to evaluate.",
                "Database", "No database code found.",
                "Architecture", "Project too small to assess.",
                "Code Quality", "Insufficient code to evaluate."
            )
        ))
    );

    @Override
    public LLMAnalysisResult analyzeRepository(RepositorySnapshot snapshot) {
        log.debug("Mock LLM analysis using fixture: {}", currentFixture);
        return FIXTURES.getOrDefault(currentFixture, LLMAnalysisResult.DEFAULT);
    }

    public void useFixture(String fixtureKey) {
        if (!FIXTURES.containsKey(fixtureKey)) {
            throw new IllegalArgumentException("Unknown fixture: " + fixtureKey);
        }
        this.currentFixture = fixtureKey;
        log.debug("Switched to fixture: {}", fixtureKey);
    }

    public String getCurrentFixture() {
        return currentFixture;
    }
}
