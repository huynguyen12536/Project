package com.learnhub.auth.oauth.exception;

/**
 * Base exception for GitHub OAuth and API operations.
 * All GitHub-related exceptions should inherit from this class.
 */
public class GitHubOAuthException extends RuntimeException {
    public GitHubOAuthException(String message) {
        super(message);
    }

    public GitHubOAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
