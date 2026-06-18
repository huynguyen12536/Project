package com.learnhub.auth.oauth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Configuration properties for GitHub OAuth integration.
 * Reads from application.yml under security.oauth.github
 */
@Component
@ConfigurationProperties(prefix = "security.oauth.github")
@Getter
@Setter
public class GitHubOAuthProperties {

    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String authUrl;
    private String tokenUrl;
    private String userUrl;
    private List<String> scopes;
    private Integer tokenExpiryBufferMinutes = 5;
    private Integer stateTokenTtlMinutes = 10;

    // From security.token-secret (not nested under github)
    private String tokenSecret;

    /**
     * Validate that all required properties are set.
     */
    public void validate() {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException("GitHub OAuth client-id is required (set GITHUB_OAUTH_CLIENT_ID)");
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException("GitHub OAuth client-secret is required (set GITHUB_OAUTH_CLIENT_SECRET)");
        }
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new IllegalStateException("GitHub OAuth redirect-uri is required (set GITHUB_OAUTH_REDIRECT_URI)");
        }
        if (tokenSecret == null || tokenSecret.isBlank()) {
            throw new IllegalStateException("GitHub token-secret is required (set GITHUB_TOKEN_SECRET)");
        }
    }
}
