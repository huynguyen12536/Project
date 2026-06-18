package com.learnhub.analysis.service;

import com.learnhub.analysis.dto.LanguageDetectionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LanguageDetectionEngine.
 * Tests language detection from file counts and extension-based detection.
 * Coverage target: 80%+
 */
class LanguageDetectionEngineTest {

    private LanguageDetectionEngine engine;

    @BeforeEach
    void setUp() {
        engine = new LanguageDetectionEngine();
    }

    // ==================== detectLanguages() Tests ====================

    @Test
    void testDetectLanguagesReturnsOrderedByFileCount() {
        // Arrange
        Map<String, Integer> languages = new HashMap<>();
        languages.put("JavaScript", 150);
        languages.put("Java", 100);
        languages.put("Python", 50);

        // Act
        List<LanguageDetectionResult> results = engine.detectLanguages(languages);

        // Assert
        assertEquals(3, results.size());
        assertEquals("JavaScript", results.get(0).getLanguage());
        assertEquals("Java", results.get(1).getLanguage());
        assertEquals("Python", results.get(2).getLanguage());
    }

    @Test
    void testDetectLanguagesCalculatesPercentageCorrectly() {
        // Arrange
        Map<String, Integer> languages = new HashMap<>();
        languages.put("Java", 50);
        languages.put("Python", 50);
        // Total = 100 files

        // Act
        List<LanguageDetectionResult> results = engine.detectLanguages(languages);

        // Assert
        assertEquals(2, results.size());
        // Each should be 50%
        LanguageDetectionResult first = results.stream()
            .filter(r -> r.getLanguage().equals("Java") || r.getLanguage().equals("Python"))
            .findFirst()
            .orElse(null);
        assertNotNull(first);
        assertEquals(50.0, first.getPercentage());
    }

    @Test
    void testDetectLanguagesWithComplexPercentage() {
        // Arrange
        Map<String, Integer> languages = new HashMap<>();
        languages.put("Java", 33);
        languages.put("JavaScript", 33);
        languages.put("Python", 34);
        // Total = 100 files

        // Act
        List<LanguageDetectionResult> results = engine.detectLanguages(languages);

        // Assert
        assertEquals(3, results.size());
        // Python should be 34%
        LanguageDetectionResult pythonResult = results.stream()
            .filter(r -> r.getLanguage().equals("Python"))
            .findFirst()
            .orElse(null);
        assertNotNull(pythonResult);
        assertEquals(34.0, pythonResult.getPercentage());
    }

    @Test
    void testDetectLanguagesHandlesEmptyMap() {
        // Arrange
        Map<String, Integer> languages = new HashMap<>();

        // Act
        List<LanguageDetectionResult> results = engine.detectLanguages(languages);

        // Assert
        assertTrue(results.isEmpty());
    }

    @Test
    void testDetectLanguagesHandlesNullMap() {
        // Act
        List<LanguageDetectionResult> results = engine.detectLanguages(null);

        // Assert
        assertTrue(results.isEmpty());
    }

    @Test
    void testDetectLanguagesIncludesEvidence() {
        // Arrange
        Map<String, Integer> languages = new HashMap<>();
        languages.put("Java", 42);

        // Act
        List<LanguageDetectionResult> results = engine.detectLanguages(languages);

        // Assert
        assertEquals(1, results.size());
        assertNotNull(results.get(0).getEvidence());
        assertEquals(1, results.get(0).getEvidence().size());
        assertEquals("42 files detected", results.get(0).getEvidence().get(0));
    }

    @Test
    void testDetectLanguagesSetsFileCount() {
        // Arrange
        Map<String, Integer> languages = new HashMap<>();
        languages.put("Java", 75);

        // Act
        List<LanguageDetectionResult> results = engine.detectLanguages(languages);

        // Assert
        assertEquals(1, results.size());
        assertEquals(75, results.get(0).getFileCount());
    }

    // ==================== detectLanguageFromExtension() Tests ====================

    @Test
    void testDetectLanguageFromExtensionJava() {
        // Act
        String language = engine.detectLanguageFromExtension("HelloWorld.java");

        // Assert
        assertEquals("Java", language);
    }

    @Test
    void testDetectLanguageFromExtensionJavaScript() {
        // Act
        String language = engine.detectLanguageFromExtension("app.js");

        // Assert
        assertEquals("JavaScript", language);
    }

    @Test
    void testDetectLanguageFromExtensionTypeScript() {
        // Act
        String language = engine.detectLanguageFromExtension("main.ts");

        // Assert
        assertEquals("TypeScript", language);
    }

    @Test
    void testDetectLanguageFromExtensionTypeScriptReact() {
        // Act
        String language = engine.detectLanguageFromExtension("Component.tsx");

        // Assert
        assertEquals("TypeScript", language);
    }

    @Test
    void testDetectLanguageFromExtensionJavaScriptReact() {
        // Act
        String language = engine.detectLanguageFromExtension("Button.jsx");

        // Assert
        assertEquals("JavaScript", language);
    }

    @Test
    void testDetectLanguageFromExtensionPython() {
        // Act
        String language = engine.detectLanguageFromExtension("script.py");

        // Assert
        assertEquals("Python", language);
    }

    @Test
    void testDetectLanguageFromExtensionGo() {
        // Act
        String language = engine.detectLanguageFromExtension("main.go");

        // Assert
        assertEquals("Go", language);
    }

    @Test
    void testDetectLanguageFromExtensionCSharp() {
        // Act
        String language = engine.detectLanguageFromExtension("Program.cs");

        // Assert
        assertEquals("C#", language);
    }

    @Test
    void testDetectLanguageFromExtensionPHP() {
        // Act
        String language = engine.detectLanguageFromExtension("index.php");

        // Assert
        assertEquals("PHP", language);
    }

    @Test
    void testDetectLanguageFromExtensionJSON() {
        // Act
        String language = engine.detectLanguageFromExtension("package.json");

        // Assert
        assertEquals("JSON", language);
    }

    @Test
    void testDetectLanguageFromExtensionSQL() {
        // Act
        String language = engine.detectLanguageFromExtension("schema.sql");

        // Assert
        assertEquals("SQL", language);
    }

    @Test
    void testDetectLanguageFromExtensionHTML() {
        // Act
        String language = engine.detectLanguageFromExtension("index.html");

        // Assert
        assertEquals("HTML", language);
    }

    @Test
    void testDetectLanguageFromExtensionCSS() {
        // Act
        String language = engine.detectLanguageFromExtension("style.css");

        // Assert
        assertEquals("CSS", language);
    }

    @Test
    void testDetectLanguageFromExtensionReturnsNullForUnknownExtension() {
        // Act
        String language = engine.detectLanguageFromExtension("file.unknown");

        // Assert
        assertNull(language);
    }

    @Test
    void testDetectLanguageFromExtensionReturnsNullForNoExtension() {
        // Act
        String language = engine.detectLanguageFromExtension("Makefile");

        // Assert
        assertNull(language);
    }

    @Test
    void testDetectLanguageFromExtensionReturnsNullForNullFilename() {
        // Act
        String language = engine.detectLanguageFromExtension(null);

        // Assert
        assertNull(language);
    }

    @Test
    void testDetectLanguageFromExtensionCaseInsensitive() {
        // Act - uppercase extension
        String language1 = engine.detectLanguageFromExtension("HelloWorld.JAVA");
        String language2 = engine.detectLanguageFromExtension("script.PY");

        // Assert
        assertEquals("Java", language1);
        assertEquals("Python", language2);
    }

    @Test
    void testDetectLanguageFromExtensionWithMultipleDots() {
        // Act
        String language = engine.detectLanguageFromExtension("config.test.js");

        // Assert
        assertEquals("JavaScript", language);
    }

    @Test
    void testDetectLanguageFromExtensionRuby() {
        // Act
        String language = engine.detectLanguageFromExtension("script.rb");

        // Assert
        assertEquals("Ruby", language);
    }

    @Test
    void testDetectLanguageFromExtensionKotlin() {
        // Act
        String language = engine.detectLanguageFromExtension("Main.kt");

        // Assert
        assertEquals("Kotlin", language);
    }

    @Test
    void testDetectLanguageFromExtensionRust() {
        // Act
        String language = engine.detectLanguageFromExtension("main.rs");

        // Assert
        assertEquals("Rust", language);
    }

    @Test
    void testDetectLanguagesWithZeroTotalFiles() {
        // Arrange
        Map<String, Integer> languages = new HashMap<>();
        languages.put("Java", 0);
        languages.put("Python", 0);

        // Act
        List<LanguageDetectionResult> results = engine.detectLanguages(languages);

        // Assert
        assertTrue(results.isEmpty());
    }

    @Test
    void testDetectLanguagesLargeScale() {
        // Arrange
        Map<String, Integer> languages = new HashMap<>();
        languages.put("Java", 10000);
        languages.put("JavaScript", 5000);
        languages.put("Python", 3000);
        languages.put("Go", 2000);
        // Total = 20000

        // Act
        List<LanguageDetectionResult> results = engine.detectLanguages(languages);

        // Assert
        assertEquals(4, results.size());
        // Java should be 50%
        assertEquals(50.0, results.get(0).getPercentage());
        // JavaScript should be 25%
        assertEquals(25.0, results.get(1).getPercentage());
        // Python should be 15%
        assertEquals(15.0, results.get(2).getPercentage());
        // Go should be 10%
        assertEquals(10.0, results.get(3).getPercentage());
    }
}
