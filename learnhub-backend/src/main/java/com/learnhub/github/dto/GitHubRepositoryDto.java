package com.learnhub.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubRepositoryDto(
    Long id,
    String name,
    String url,
    String description,
    String language,
    @JsonProperty("last_updated")
    String lastUpdated,
    @JsonProperty("size_kb")
    Integer sizeKb,
    @JsonProperty("is_private")
    Boolean isPrivate,
    @JsonProperty("is_fork")
    Boolean isFork
) {
    public GitHubRepositoryDto {
        // Sanitize text fields to prevent XSS
        // Truncate and escape description
        if (description != null) {
            if (description.length() > 500) {
                description = description.substring(0, 500);
            }
            description = escapeHtml(description);
        }

        // Escape repository name (though unlikely to have HTML, defense-in-depth)
        if (name != null) {
            name = escapeHtml(name);
        }

        // Escape language (same rationale)
        if (language != null) {
            language = escapeHtml(language);
        }
    }

    /**
     * Escape HTML special characters to prevent XSS injection.
     * Converts special characters to HTML entities.
     *
     * @param text the text to escape
     * @return escaped text safe for HTML context
     */
    private static String escapeHtml(String text) {
        if (text == null) return null;
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
