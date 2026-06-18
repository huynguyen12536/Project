package com.learnhub.auth.oauth.exception;

/**
 * Thrown when GitHub token refresh fails.
 */
public class TokenRefreshException extends GitHubOAuthException {

    public TokenRefreshException(String message) {
        super(message);
    }

    public TokenRefreshException(String message, Throwable cause) {
        super(message, cause);
    }
}
