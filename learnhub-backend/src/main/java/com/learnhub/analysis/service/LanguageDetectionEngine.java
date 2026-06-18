package com.learnhub.analysis.service;

import com.learnhub.analysis.dto.LanguageDetectionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Component responsible for detecting programming languages in a repository.
 * Uses file extension mapping for deterministic language detection.
 * Supports 29+ file extensions across 7 primary languages.
 */
@Component
@Slf4j
public class LanguageDetectionEngine {

    // File extension mappings to language names
    private static final Map<String, String> EXTENSION_TO_LANGUAGE = Map.ofEntries(
        // Java ecosystem
        Map.entry("java", "Java"),

        // JavaScript/TypeScript ecosystem
        Map.entry("js", "JavaScript"),
        Map.entry("ts", "TypeScript"),
        Map.entry("tsx", "TypeScript"),
        Map.entry("jsx", "JavaScript"),

        // Python ecosystem
        Map.entry("py", "Python"),

        // Go ecosystem
        Map.entry("go", "Go"),

        // C# ecosystem
        Map.entry("cs", "C#"),

        // PHP ecosystem
        Map.entry("php", "PHP"),

        // Ruby ecosystem
        Map.entry("rb", "Ruby"),

        // Swift ecosystem
        Map.entry("swift", "Swift"),

        // Kotlin ecosystem
        Map.entry("kt", "Kotlin"),

        // Scala ecosystem
        Map.entry("scala", "Scala"),

        // Rust ecosystem
        Map.entry("rs", "Rust"),

        // C++ ecosystem
        Map.entry("cpp", "C++"),
        Map.entry("hpp", "C++"),

        // C ecosystem
        Map.entry("c", "C"),
        Map.entry("h", "C"),

        // Markup/Config languages
        Map.entry("xml", "XML"),
        Map.entry("json", "JSON"),
        Map.entry("yaml", "YAML"),
        Map.entry("yml", "YAML"),
        Map.entry("sql", "SQL"),
        Map.entry("html", "HTML"),
        Map.entry("css", "CSS"),
        Map.entry("scss", "SCSS"),
        Map.entry("less", "LESS"),

        // Shell scripting
        Map.entry("sh", "Shell"),
        Map.entry("bash", "Bash")
    );

    /**
     * Detects programming languages from a map of language names to file counts.
     * Calculates percentages and sorts results by file count in descending order.
     *
     * @param languages Map where key is language name and value is file count
     * @return List of LanguageDetectionResult objects sorted by file count (descending)
     */
    public List<LanguageDetectionResult> detectLanguages(Map<String, Integer> languages) {
        if (languages == null || languages.isEmpty()) {
            log.debug("No languages provided for detection");
            return List.of();
        }

        int totalFiles = languages.values().stream()
            .mapToInt(Integer::intValue)
            .sum();

        if (totalFiles == 0) {
            log.debug("Total file count is zero");
            return List.of();
        }

        List<LanguageDetectionResult> results = languages.entrySet().stream()
            .map(entry -> {
                String language = entry.getKey();
                Integer fileCount = entry.getValue();
                double percentage = (double) fileCount / totalFiles * 100;

                LanguageDetectionResult result = new LanguageDetectionResult();
                result.setLanguage(language);
                result.setFileCount(fileCount);
                // Round to 2 decimal places
                result.setPercentage(Math.round(percentage * 100.0) / 100.0);
                result.setEvidence(List.of(fileCount + " files detected"));

                return result;
            })
            .sorted(Comparator.comparingInt(LanguageDetectionResult::getFileCount).reversed())
            .collect(Collectors.toList());

        log.debug("Detected {} languages from {} files", results.size(), totalFiles);
        return results;
    }

    /**
     * Detects the programming language from a file extension.
     * Uses case-insensitive matching against known extension mappings.
     *
     * @param filename File name to analyze
     * @return Language name if extension is recognized, null otherwise
     */
    public String detectLanguageFromExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            log.trace("Invalid filename for extension detection: {}", filename);
            return null;
        }

        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        String language = EXTENSION_TO_LANGUAGE.getOrDefault(extension, null);

        if (language != null) {
            log.trace("File {} detected as {} language", filename, language);
        }

        return language;
    }
}
