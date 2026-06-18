package com.learnhub.analysis.service;

import com.learnhub.analysis.dto.LanguageDetectionResult;
import com.learnhub.analysis.dto.SkillScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SkillExtractionEngine.
 * Tests skill extraction from language detection results and package information.
 * Coverage target: 80%+
 */
class SkillExtractionEngineTest {

    private SkillExtractionEngine engine;

    @BeforeEach
    void setUp() {
        engine = new SkillExtractionEngine();
    }

    // ==================== Java Skills Tests ====================

    @Test
    void testExtractJavaSkillsDetectsSpringBoot() {
        // Arrange
        List<LanguageDetectionResult> languages = new ArrayList<>();
        LanguageDetectionResult javaResult = new LanguageDetectionResult();
        javaResult.setLanguage("Java");
        languages.add(javaResult);
        String packageInfo = "spring-boot @SpringBootApplication pom.xml";

        // Act
        List<SkillScore> skills = engine.extractSkills(languages, packageInfo);

        // Assert
        SkillScore springBoot = skills.stream()
            .filter(s -> s.getSkillName().equals("Spring Boot"))
            .findFirst()
            .orElse(null);
        assertNotNull(springBoot);
        assertTrue(springBoot.getConfidence() > 0);
        assertEquals("Java", springBoot.getLanguage());
    }

    @Test
    void testExtractJavaSkillsDetectsJPA() {
        // Arrange
        List<LanguageDetectionResult> languages = new ArrayList<>();
        LanguageDetectionResult javaResult = new LanguageDetectionResult();
        javaResult.setLanguage("Java");
        languages.add(javaResult);
        String packageInfo = "jakarta.persistence @Entity @Repository";

        // Act
        List<SkillScore> skills = engine.extractSkills(languages, packageInfo);

        // Assert
        SkillScore jpa = skills.stream()
            .filter(s -> s.getSkillName().equals("JPA"))
            .findFirst()
            .orElse(null);
        assertNotNull(jpa);
        assertTrue(jpa.getConfidence() > 0);
    }

    @Test
    void testExtractJavaSkillsDetectsMaven() {
        // Arrange
        List<LanguageDetectionResult> languages = new ArrayList<>();
        LanguageDetectionResult javaResult = new LanguageDetectionResult();
        javaResult.setLanguage("Java");
        languages.add(javaResult);
        String packageInfo = "pom.xml <dependency>";

        // Act
        List<SkillScore> skills = engine.extractSkills(languages, packageInfo);

        // Assert
        SkillScore maven = skills.stream()
            .filter(s -> s.getSkillName().equals("Maven"))
            .findFirst()
            .orElse(null);
        assertNotNull(maven);
        assertEquals(100, maven.getConfidence());
    }

    @Test
    void testExtractJavaSkillsDetectsLombok() {
        // Arrange
        List<LanguageDetectionResult> languages = new ArrayList<>();
        LanguageDetectionResult javaResult = new LanguageDetectionResult();
        javaResult.setLanguage("Java");
        languages.add(javaResult);
        String packageInfo = "@Data @Getter @Setter lombok";

        // Act
        List<SkillScore> skills = engine.extractSkills(languages, packageInfo);

        // Assert
        SkillScore lombok = skills.stream()
            .filter(s -> s.getSkillName().equals("Lombok"))
            .findFirst()
            .orElse(null);
        assertNotNull(lombok);
        assertTrue(lombok.getConfidence() > 0);
    }

    @Test
    void testExtractJavaSkillsDetectsJUnit() {
        // Arrange
        List<LanguageDetectionResult> languages = new ArrayList<>();
        LanguageDetectionResult javaResult = new LanguageDetectionResult();
        javaResult.setLanguage("Java");
        languages.add(javaResult);
        String packageInfo = "junit @Test";

        // Act
        List<SkillScore> skills = engine.extractSkills(languages, packageInfo);

        // Assert
        SkillScore junit = skills.stream()
            .filter(s -> s.getSkillName().equals("JUnit"))
            .findFirst()
            .orElse(null);
        assertNotNull(junit);
        assertTrue(junit.getConfidence() > 0);
    }

    // ==================== JavaScript Skills Tests ====================

    @Test
    void testExtractJavaScriptSkillsDetectsReact() {
        // Arrange
        List<LanguageDetectionResult> languages = new ArrayList<>();
        LanguageDetectionResult jsResult = new LanguageDetectionResult();
        jsResult.setLanguage("JavaScript");
        languages.add(jsResult);
        String packageInfo = "react from 'react' useState useEffect";

        // Act
        List<SkillScore> skills = engine.extractSkills(languages, packageInfo);

        // Assert
        SkillScore react = skills.stream()
            .filter(s -> s.getSkillName().equals("React"))
            .findFirst()
            .orElse(null);
        assertNotNull(react);
        assertTrue(react.getConfidence() > 0);
        assertEquals("JavaScript", react.getLanguage());
    }

    @Test
    void testExtractJavaScriptSkillsDetectsExpress() {
        // Arrange
        List<LanguageDetectionResult> languages = new ArrayList<>();
        LanguageDetectionResult jsResult = new LanguageDetectionResult();
        jsResult.setLanguage("JavaScript");
        languages.add(jsResult);
        String packageInfo = "express app.get app.post middleware";

        // Act
        List<SkillScore> skills = engine.extractSkills(languages, packageInfo);

        // Assert
        SkillScore express = skills.stream()
            .filter(s -> s.getSkillName().equals("Express"))
            .findFirst()
            .orElse(null);
        assertNotNull(express);
        assertEquals(100, express.getConfidence());
    }

    @Test
    void testExtractJavaScriptSkillsDetectsNodejs() {
        // Arrange
        List<LanguageDetectionResult> languages = new ArrayList<>();
        LanguageDetectionResult jsResult = new LanguageDetectionResult();
        jsResult.setLanguage("JavaScript");
        languages.add(jsResult);
        String packageInfo = "node require module.exports npm";

        // Act
        List<SkillScore> skills = engine.extractSkills(languages, packageInfo);

        // Assert
        SkillScore nodejs = skills.stream()
            .filter(s -> s.getSkillName().equals("Node.js"))
            .findFirst()
            .orElse(null);
        assertNotNull(nodejs);
        assertEquals(100, nodejs.getConfidence());
    }

    @Test
    void testExtractJavaScriptSkillsDetectsJest() {
        // Arrange
        List<LanguageDetectionResult> languages = new ArrayList<>();
        LanguageDetectionResult jsResult = new LanguageDetectionResult();
        jsResult.setLanguage("JavaScript");
        languages.add(jsResult);
        String packageInfo = "jest describe( it( test(";

        // Act
        List<SkillScore> skills = engine.extractSkills(languages, packageInfo);

        // Assert
        SkillScore jest = skills.stream()
            .filter(s -> s.getSkillName().equals("Jest"))
            .findFirst()
            .orElse(null);
        assertNotNull(jest);
        assertTrue(jest.getConfidence() > 0);
    }

    // ==================== Python Skills Tests ====================

    @Test
    void testExtractPythonSkillsDetectsDjango() {
        // Arrange
        List<LanguageDetectionResult> languages = new ArrayList<>();
        LanguageDetectionResult pyResult = new LanguageDetectionResult();
        pyResult.setLanguage("Python");
        languages.add(pyResult);
        String packageInfo = "django from django models.py views.py";

        // Act
        List<SkillScore> skills = engine.extractSkills(languages, packageInfo);

        // Assert
        SkillScore django = skills.stream()
            .filter(s -> s.getSkillName().equals("Django"))
            .findFirst()
            .orElse(null);
        assertNotNull(django);
        assertTrue(django.getConfidence() > 0);
        assertEquals("Python", django.getLanguage());
    }

    @Test
    void testExtractPythonSkillsDetectsFlask() {
        // Arrange
        List<LanguageDetectionResult> languages = new ArrayList<>();
        LanguageDetectionResult pyResult = new LanguageDetectionResult();
        pyResult.setLanguage("Python");
        languages.add(pyResult);
        String packageInfo = "flask from flask @app.route";

        // Act
        List<SkillScore> skills = engine.extractSkills(languages, packageInfo);

        // Assert
        SkillScore flask = skills.stream()
            .filter(s -> s.getSkillName().equals("Flask"))
            .findFirst()
            .orElse(null);
        assertNotNull(flask);
        assertEquals(100, flask.getConfidence());
    }

    @Test
    void testExtractPythonSkillsDetectsPytest() {
        // Arrange
        List<LanguageDetectionResult> languages = new ArrayList<>();
        LanguageDetectionResult pyResult = new LanguageDetectionResult();
        pyResult.setLanguage("Python");
        languages.add(pyResult);
        String packageInfo = "pytest def test_ @pytest";

        // Act
        List<SkillScore> skills = engine.extractSkills(languages, packageInfo);

        // Assert
        SkillScore pytest = skills.stream()
            .filter(s -> s.getSkillName().equals("pytest"))
            .findFirst()
            .orElse(null);
        assertNotNull(pytest);
        assertEquals(100, pytest.getConfidence());
    }

    @Test
    void testExtractPythonSkillsDetectsPandas() {
        // Arrange
        List<LanguageDetectionResult> languages = new ArrayList<>();
        LanguageDetectionResult pyResult = new LanguageDetectionResult();
        pyResult.setLanguage("Python");
        languages.add(pyResult);
        String packageInfo = "pandas import pandas";

        // Act
        List<SkillScore> skills = engine.extractSkills(languages, packageInfo);

        // Assert
        SkillScore pandas = skills.stream()
            .filter(s -> s.getSkillName().equals("Pandas"))
            .findFirst()
            .orElse(null);
        assertNotNull(pandas);
        assertEquals(100, pandas.getConfidence());
    }

    // ==================== Confidence Scoring Tests ====================

    @Test
    void testConfidenceScoringWithPartialMatches() {
        // Arrange
        List<LanguageDetectionResult> languages = new ArrayList<>();
        LanguageDetectionResult javaResult = new LanguageDetectionResult();
        javaResult.setLanguage("Java");
        languages.add(javaResult);
        // Spring Boot has 3 indicators: spring-boot, @SpringBootApplication, spring.boot
        // Providing only 2 out of 3
        String packageInfo = "spring-boot @SpringBootApplication";

        // Act
        List<SkillScore> skills = engine.extractSkills(languages, packageInfo);

        // Assert
        SkillScore springBoot = skills.stream()
            .filter(s -> s.getSkillName().equals("Spring Boot"))
            .findFirst()
            .orElse(null);
        assertNotNull(springBoot);
        // 2/3 * 100 = 66
        assertEquals(66, springBoot.getConfidence());
    }

    @Test
    void testConfidenceScoringWithAllMatches() {
        // Arrange
        List<LanguageDetectionResult> languages = new ArrayList<>();
        LanguageDetectionResult jsResult = new LanguageDetectionResult();
        jsResult.setLanguage("JavaScript");
        languages.add(jsResult);
        // npm has 2 indicators: package.json, npm install
        String packageInfo = "package.json npm install";

        // Act
        List<SkillScore> skills = engine.extractSkills(languages, packageInfo);

        // Assert
        SkillScore npm = skills.stream()
            .filter(s -> s.getSkillName().equals("npm"))
            .findFirst()
            .orElse(null);
        assertNotNull(npm);
        assertEquals(100, npm.getConfidence());
    }

    @Test
    void testConfidenceScoringRange() {
        // Arrange
        List<LanguageDetectionResult> languages = new ArrayList<>();
        LanguageDetectionResult javaResult = new LanguageDetectionResult();
        javaResult.setLanguage("Java");
        languages.add(javaResult);
        String packageInfo = "spring-boot";

        // Act
        List<SkillScore> skills = engine.extractSkills(languages, packageInfo);

        // Assert
        for (SkillScore skill : skills) {
            assertTrue(skill.getConfidence() >= 0);
            assertTrue(skill.getConfidence() <= 100);
        }
    }

    // ==================== Filtering Tests ====================

    @Test
    void testExtractsSkillsFiltersOutZeroConfidenceSkills() {
        // Arrange
        List<LanguageDetectionResult> languages = new ArrayList<>();
        LanguageDetectionResult javaResult = new LanguageDetectionResult();
        javaResult.setLanguage("Java");
        languages.add(javaResult);
        // Providing no indicators for any skill
        String packageInfo = "random text";

        // Act
        List<SkillScore> skills = engine.extractSkills(languages, packageInfo);

        // Assert
        assertTrue(skills.isEmpty());
    }

    @Test
    void testExtractsSkillsHandlesEmptyLanguageList() {
        // Arrange
        List<LanguageDetectionResult> languages = new ArrayList<>();
        String packageInfo = "spring-boot react django";

        // Act
        List<SkillScore> skills = engine.extractSkills(languages, packageInfo);

        // Assert
        assertTrue(skills.isEmpty());
    }

    @Test
    void testExtractsSkillsHandlesNullLanguageList() {
        // Act
        List<SkillScore> skills = engine.extractSkills(null, "spring-boot");

        // Assert
        assertTrue(skills.isEmpty());
    }

    @Test
    void testExtractsSkillsHandlesNullPackageInfo() {
        // Arrange
        List<LanguageDetectionResult> languages = new ArrayList<>();
        LanguageDetectionResult javaResult = new LanguageDetectionResult();
        javaResult.setLanguage("Java");
        languages.add(javaResult);

        // Act
        List<SkillScore> skills = engine.extractSkills(languages, null);

        // Assert
        assertTrue(skills.isEmpty());
    }

    // ==================== TypeScript Skills Tests ====================

    @Test
    void testExtractTypeScriptSkillsDetectsTypeScript() {
        // Arrange
        List<LanguageDetectionResult> languages = new ArrayList<>();
        LanguageDetectionResult tsResult = new LanguageDetectionResult();
        tsResult.setLanguage("TypeScript");
        languages.add(tsResult);
        String packageInfo = ".ts .tsx interface type";

        // Act
        List<SkillScore> skills = engine.extractSkills(languages, packageInfo);

        // Assert
        SkillScore ts = skills.stream()
            .filter(s -> s.getSkillName().equals("TypeScript"))
            .findFirst()
            .orElse(null);
        assertNotNull(ts);
        assertTrue(ts.getConfidence() > 0);
        assertEquals("TypeScript", ts.getLanguage());
    }

    @Test
    void testExtractTypeScriptSkillsDetectsAngular() {
        // Arrange
        List<LanguageDetectionResult> languages = new ArrayList<>();
        LanguageDetectionResult tsResult = new LanguageDetectionResult();
        tsResult.setLanguage("TypeScript");
        languages.add(tsResult);
        String packageInfo = "@angular NgModule Component";

        // Act
        List<SkillScore> skills = engine.extractSkills(languages, packageInfo);

        // Assert
        SkillScore angular = skills.stream()
            .filter(s -> s.getSkillName().equals("Angular"))
            .findFirst()
            .orElse(null);
        assertNotNull(angular);
        assertTrue(angular.getConfidence() > 0);
    }

    // ==================== Go Skills Tests ====================

    @Test
    void testExtractGoSkillsDetectsGoroutines() {
        // Arrange
        List<LanguageDetectionResult> languages = new ArrayList<>();
        LanguageDetectionResult goResult = new LanguageDetectionResult();
        goResult.setLanguage("Go");
        languages.add(goResult);
        String packageInfo = "go goroutine";

        // Act
        List<SkillScore> skills = engine.extractSkills(languages, packageInfo);

        // Assert
        SkillScore goroutines = skills.stream()
            .filter(s -> s.getSkillName().equals("Goroutines"))
            .findFirst()
            .orElse(null);
        assertNotNull(goroutines);
        assertEquals(100, goroutines.getConfidence());
    }

    // ==================== PHP Skills Tests ====================

    @Test
    void testExtractPHPSkillsDetectsLaravel() {
        // Arrange
        List<LanguageDetectionResult> languages = new ArrayList<>();
        LanguageDetectionResult phpResult = new LanguageDetectionResult();
        phpResult.setLanguage("PHP");
        languages.add(phpResult);
        String packageInfo = "laravel artisan routes.php";

        // Act
        List<SkillScore> skills = engine.extractSkills(languages, packageInfo);

        // Assert
        SkillScore laravel = skills.stream()
            .filter(s -> s.getSkillName().equals("Laravel"))
            .findFirst()
            .orElse(null);
        assertNotNull(laravel);
        assertTrue(laravel.getConfidence() > 0);
    }

    // ==================== Evidence Tests ====================

    @Test
    void testSkillScoreIncludesEvidence() {
        // Arrange
        List<LanguageDetectionResult> languages = new ArrayList<>();
        LanguageDetectionResult javaResult = new LanguageDetectionResult();
        javaResult.setLanguage("Java");
        languages.add(javaResult);
        String packageInfo = "pom.xml";

        // Act
        List<SkillScore> skills = engine.extractSkills(languages, packageInfo);

        // Assert
        SkillScore maven = skills.stream()
            .filter(s -> s.getSkillName().equals("Maven"))
            .findFirst()
            .orElse(null);
        assertNotNull(maven);
        assertNotNull(maven.getEvidence());
        assertTrue(maven.getEvidence().contains("2"));
    }

    // ==================== Multiple Languages Tests ====================

    @Test
    void testExtractSkillsWithMultipleLanguages() {
        // Arrange
        List<LanguageDetectionResult> languages = new ArrayList<>();
        LanguageDetectionResult javaResult = new LanguageDetectionResult();
        javaResult.setLanguage("Java");
        LanguageDetectionResult jsResult = new LanguageDetectionResult();
        jsResult.setLanguage("JavaScript");
        languages.add(javaResult);
        languages.add(jsResult);
        String packageInfo = "spring-boot react pom.xml package.json";

        // Act
        List<SkillScore> skills = engine.extractSkills(languages, packageInfo);

        // Assert
        assertTrue(skills.size() >= 2);
        SkillScore springBoot = skills.stream()
            .filter(s -> s.getSkillName().equals("Spring Boot"))
            .findFirst()
            .orElse(null);
        SkillScore react = skills.stream()
            .filter(s -> s.getSkillName().equals("React"))
            .findFirst()
            .orElse(null);
        assertNotNull(springBoot);
        assertNotNull(react);
    }

    @Test
    void testExtractSkillsCaseSensitivity() {
        // Arrange
        List<LanguageDetectionResult> languages = new ArrayList<>();
        LanguageDetectionResult javaResult = new LanguageDetectionResult();
        javaResult.setLanguage("Java");
        languages.add(javaResult);
        // Testing with uppercase - should match because indicators are compared in lowercase
        String packageInfo = "SPRING-BOOT POMERXML";

        // Act
        List<SkillScore> skills = engine.extractSkills(languages, packageInfo);

        // Assert
        // Should match spring-boot due to case-insensitive comparison
        SkillScore springBoot = skills.stream()
            .filter(s -> s.getSkillName().equals("Spring Boot"))
            .findFirst()
            .orElse(null);
        assertNotNull(springBoot);
    }

    @Test
    void testExtractSkillsDistinct() {
        // This test verifies that duplicate skills are not returned
        // The service should return distinct skills only
        // Arrange
        List<LanguageDetectionResult> languages = new ArrayList<>();
        LanguageDetectionResult javaResult = new LanguageDetectionResult();
        javaResult.setLanguage("Java");
        languages.add(javaResult);
        String packageInfo = "spring-boot @SpringBootApplication spring.boot spring-boot";

        // Act
        List<SkillScore> skills = engine.extractSkills(languages, packageInfo);

        // Assert
        long springBootCount = skills.stream()
            .filter(s -> s.getSkillName().equals("Spring Boot"))
            .count();
        assertEquals(1, springBootCount);
    }
}
