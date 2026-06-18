package com.learnhub.auth.oauth.exception;

/**
 * Exception for GitHub token refresh failures.
 * Thrown when refresh token exchange fails or is invalid.
 */
public class TokenRefreshException extends GitHubOAuthException {
    public TokenRefreshException(String message) {
        super(message);
    }

    public TokenRefreshException(String message, Throwable cause) {
        super(message, cause);
    }
}
