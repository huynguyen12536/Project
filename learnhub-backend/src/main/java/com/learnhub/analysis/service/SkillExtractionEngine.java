package com.learnhub.analysis.service;

import com.learnhub.analysis.dto.LanguageDetectionResult;
import com.learnhub.analysis.dto.SkillScore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Component responsible for extracting skills from repository metadata.
 * Uses deterministic indicator-based scoring with no AI/ML components.
 * Confidence is calculated as: (matched_indicators / total_indicators) * 100
 *
 * Supports skill extraction for 7 primary languages:
 * - Java: Spring Boot, JPA, Hibernate, Maven, Gradle, JUnit, Mockito, Lombok
 * - JavaScript: React, Express, Node.js, Webpack, Babel, npm, Jest
 * - TypeScript: TypeScript, Angular, NestJS, tsc
 * - Python: Django, Flask, FastAPI, Pandas, NumPy, pytest, Celery
 * - Go: Goroutines, Gin, gRPC, Database/SQL, Redis
 * - C#: ASP.NET Core, Entity Framework, LINQ, NUnit, xUnit, Async/Await
 * - PHP: Laravel, Symfony, Composer, PHPUnit, Blade, Eloquent
 */
@Component
@Slf4j
public class SkillExtractionEngine {

    // Java ecosystem skills and their indicators
    private static final Map<String, List<String>> JAVA_SKILLS = Map.of(
        "Spring Boot", List.of("spring-boot", "@SpringBootApplication", "spring.boot"),
        "JPA", List.of("javax.persistence", "jakarta.persistence", "@Entity", "@Repository"),
        "Hibernate", List.of("hibernate", "@Transactional", "Session"),
        "Maven", List.of("pom.xml", "<dependency>"),
        "Gradle", List.of("build.gradle", "gradle"),
        "JUnit", List.of("junit", "@Test", "@RunWith"),
        "Mockito", List.of("mockito", "@Mock", "when("),
        "Lombok", List.of("@Data", "@Entity", "@Getter", "@Setter", "lombok")
    );

    // JavaScript ecosystem skills and their indicators
    private static final Map<String, List<String>> JAVASCRIPT_SKILLS = Map.of(
        "React", List.of("react", "from 'react'", "useState", "useEffect", "Component"),
        "Express", List.of("express", "app.get", "app.post", "middleware"),
        "Node.js", List.of("node", "require", "module.exports", "npm"),
        "Webpack", List.of("webpack", "webpack.config"),
        "Babel", List.of("babel", ".babelrc"),
        "npm", List.of("package.json", "npm install"),
        "Jest", List.of("jest", "describe(", "it(", "test(")
    );

    // TypeScript ecosystem skills and their indicators
    private static final Map<String, List<String>> TYPESCRIPT_SKILLS = Map.of(
        "TypeScript", List.of(".ts", ".tsx", "interface", "type", ": string", ": number"),
        "Angular", List.of("@angular", "NgModule", "Component", "Injectable"),
        "NestJS", List.of("@nestjs", "@Controller", "@Module"),
        "tsc", List.of("tsconfig.json", "tsc")
    );

    // Python ecosystem skills and their indicators
    private static final Map<String, List<String>> PYTHON_SKILLS = Map.of(
        "Django", List.of("django", "from django", "models.py", "views.py"),
        "Flask", List.of("flask", "from flask", "@app.route"),
        "FastAPI", List.of("fastapi", "from fastapi", "@app.get", "@app.post"),
        "Pandas", List.of("pandas", "import pandas"),
        "NumPy", List.of("numpy", "import numpy"),
        "pytest", List.of("pytest", "def test_", "@pytest"),
        "Celery", List.of("celery", "from celery", "@task")
    );

    // Go ecosystem skills and their indicators
    private static final Map<String, List<String>> GO_SKILLS = Map.of(
        "Goroutines", List.of("go ", "goroutine"),
        "Gin", List.of("gin", "gin.New()", "gin.Engine"),
        "gRPC", List.of("grpc", "proto"),
        "Database/SQL", List.of("database/sql", "sql.DB"),
        "Redis", List.of("redis", "redisClient")
    );

    // C# ecosystem skills and their indicators
    private static final Map<String, List<String>> CSHARP_SKILLS = Map.of(
        "ASP.NET Core", List.of("ASP.NET", "Startup.cs", "Middleware"),
        "Entity Framework", List.of("DbContext", "DbSet", "EntityFramework"),
        "LINQ", List.of("from ", "select ", ".Where(", ".Select("),
        "NUnit", List.of("NUnit", "[Test]", "Assert"),
        "xUnit", List.of("xUnit", "[Fact]", "[Theory]"),
        "Async/Await", List.of("async", "await", "Task<")
    );

    // PHP ecosystem skills and their indicators
    private static final Map<String, List<String>> PHP_SKILLS = Map.of(
        "Laravel", List.of("laravel", "artisan", "routes.php", "Illuminate"),
        "Symfony", List.of("symfony", "Symfony\\"),
        "Composer", List.of("composer.json", "vendor/"),
        "PHPUnit", List.of("phpunit", "testCase"),
        "Blade", List.of("blade", ".blade.php"),
        "Eloquent", List.of("eloquent", "Model")
    );

    /**
     * Extracts skills from detected languages and package information.
     * Processes each detected language and extracts relevant skills.
     *
     * @param languages List of LanguageDetectionResult objects
     * @param packageInfo Concatenated package/dependency information
     * @return List of distinct SkillScore objects (filtered to confidence > 0)
     */
    public List<SkillScore> extractSkills(List<LanguageDetectionResult> languages, String packageInfo) {
        if (languages == null || languages.isEmpty()) {
            log.debug("No languages provided for skill extraction");
            return List.of();
        }

        List<SkillScore> skills = new ArrayList<>();

        for (LanguageDetectionResult langResult : languages) {
            String language = langResult.getLanguage();

            switch (language.toLowerCase()) {
                case "java":
                    skills.addAll(extractJavaSkills(packageInfo));
                    break;
                case "javascript":
                    skills.addAll(extractJavaScriptSkills(packageInfo));
                    break;
                case "typescript":
                    skills.addAll(extractTypeScriptSkills(packageInfo));
                    break;
                case "python":
                    skills.addAll(extractPythonSkills(packageInfo));
                    break;
                case "go":
                    skills.addAll(extractGoSkills(packageInfo));
                    break;
                case "c#":
                    skills.addAll(extractCSharpSkills(packageInfo));
                    break;
                case "php":
                    skills.addAll(extractPHPSkills(packageInfo));
                    break;
                default:
                    log.trace("No skill extraction rules defined for language: {}", language);
            }
        }

        List<SkillScore> distinctSkills = skills.stream()
            .distinct()
            .collect(Collectors.toList());

        log.debug("Extracted {} unique skills", distinctSkills.size());
        return distinctSkills;
    }

    /**
     * Extracts Java ecosystem skills.
     *
     * @param packageInfo Package information string
     * @return List of detected Java skills
     */
    private List<SkillScore> extractJavaSkills(String packageInfo) {
        return JAVA_SKILLS.entrySet().stream()
            .map(entry -> scoreSkill(entry.getKey(), entry.getValue(), packageInfo, "Java"))
            .filter(skill -> skill.getConfidence() > 0)
            .collect(Collectors.toList());
    }

    /**
     * Extracts JavaScript ecosystem skills.
     *
     * @param packageInfo Package information string
     * @return List of detected JavaScript skills
     */
    private List<SkillScore> extractJavaScriptSkills(String packageInfo) {
        return JAVASCRIPT_SKILLS.entrySet().stream()
            .map(entry -> scoreSkill(entry.getKey(), entry.getValue(), packageInfo, "JavaScript"))
            .filter(skill -> skill.getConfidence() > 0)
            .collect(Collectors.toList());
    }

    /**
     * Extracts TypeScript ecosystem skills.
     *
     * @param packageInfo Package information string
     * @return List of detected TypeScript skills
     */
    private List<SkillScore> extractTypeScriptSkills(String packageInfo) {
        return TYPESCRIPT_SKILLS.entrySet().stream()
            .map(entry -> scoreSkill(entry.getKey(), entry.getValue(), packageInfo, "TypeScript"))
            .filter(skill -> skill.getConfidence() > 0)
            .collect(Collectors.toList());
    }

    /**
     * Extracts Python ecosystem skills.
     *
     * @param packageInfo Package information string
     * @return List of detected Python skills
     */
    private List<SkillScore> extractPythonSkills(String packageInfo) {
        return PYTHON_SKILLS.entrySet().stream()
            .map(entry -> scoreSkill(entry.getKey(), entry.getValue(), packageInfo, "Python"))
            .filter(skill -> skill.getConfidence() > 0)
            .collect(Collectors.toList());
    }

    /**
     * Extracts Go ecosystem skills.
     *
     * @param packageInfo Package information string
     * @return List of detected Go skills
     */
    private List<SkillScore> extractGoSkills(String packageInfo) {
        return GO_SKILLS.entrySet().stream()
            .map(entry -> scoreSkill(entry.getKey(), entry.getValue(), packageInfo, "Go"))
            .filter(skill -> skill.getConfidence() > 0)
            .collect(Collectors.toList());
    }

    /**
     * Extracts C# ecosystem skills.
     *
     * @param packageInfo Package information string
     * @return List of detected C# skills
     */
    private List<SkillScore> extractCSharpSkills(String packageInfo) {
        return CSHARP_SKILLS.entrySet().stream()
            .map(entry -> scoreSkill(entry.getKey(), entry.getValue(), packageInfo, "C#"))
            .filter(skill -> skill.getConfidence() > 0)
            .collect(Collectors.toList());
    }

    /**
     * Extracts PHP ecosystem skills.
     *
     * @param packageInfo Package information string
     * @return List of detected PHP skills
     */
    private List<SkillScore> extractPHPSkills(String packageInfo) {
        return PHP_SKILLS.entrySet().stream()
            .map(entry -> scoreSkill(entry.getKey(), entry.getValue(), packageInfo, "PHP"))
            .filter(skill -> skill.getConfidence() > 0)
            .collect(Collectors.toList());
    }

    /**
     * Scores a skill based on matched indicators.
     * Confidence = (matched_indicators / total_indicators) * 100
     *
     * @param skillName   Name of the skill
     * @param indicators  List of indicator strings to match
     * @param packageInfo Package information to search within
     * @param language    Primary language for the skill
     * @return SkillScore with calculated confidence
     */
    private SkillScore scoreSkill(String skillName, List<String> indicators, String packageInfo, String language) {
        int matchCount = 0;
        String normalizedInfo = packageInfo != null ? packageInfo.toLowerCase() : "";

        for (String indicator : indicators) {
            if (normalizedInfo.contains(indicator.toLowerCase())) {
                matchCount++;
            }
        }

        // Confidence = (matches / total indicators) * 100
        int confidence = matchCount > 0
            ? (matchCount * 100) / indicators.size()
            : 0;

        String evidence = matchCount + "/" + indicators.size() + " indicators found";

        return new SkillScore(skillName, confidence, evidence, language);
    }
}
