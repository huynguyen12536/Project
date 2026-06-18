package com.learnhub.auth.oauth.exception;

/**
 * Thrown when attempting to link a GitHub account that's already connected to another LearnHub user.
 * Returns HTTP 409 Conflict.
 */
public class DuplicateGitHubUserException extends GitHubOAuthException {

    public DuplicateGitHubUserException(String message) {
        super(message);
    }

    public DuplicateGitHubUserException(String message, Throwable cause) {
        super(message, cause);
    }
}
