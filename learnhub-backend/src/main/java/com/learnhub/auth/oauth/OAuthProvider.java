package com.learnhub.auth.oauth;

/**
 * Supported OAuth providers for user authentication and authorization.
 * Currently supports GitHub; extensible for future providers (Google, GitLab).
 */
public enum OAuthProvider {
    GITHUB("github"),
    GOOGLE("google"),
    GITLAB("gitlab");

    private final String displayName;

    OAuthProvider(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Find provider by display name (case-insensitive).
     */
    public static OAuthProvider fromDisplayName(String displayName) {
        for (OAuthProvider provider : values()) {
            if (provider.displayName.equalsIgnoreCase(displayName)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unknown OAuth provider: " + displayName);
    }
}
