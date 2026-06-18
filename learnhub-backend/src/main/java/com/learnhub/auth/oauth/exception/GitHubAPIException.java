package com.learnhub.auth.oauth.exception;

/**
 * Exception for GitHub API errors.
 * Thrown when GitHub API calls fail (non-rate-limit errors).
 */
public class GitHubAPIException extends GitHubOAuthException {
    public GitHubAPIException(String message) {
        super(message);
    }

    public GitHubAPIException(String message, Throwable cause) {
        super(message, cause);
    }
}
