package com.learnhub.auth.oauth.exception;

/**
 * Thrown when GitHub API call fails (network error, timeout, etc).
 * Returns HTTP 502 Bad Gateway.
 */
public class GitHubAPIException extends GitHubOAuthException {

    public GitHubAPIException(String message) {
        super(message);
    }

    public GitHubAPIException(String message, Throwable cause) {
        super(message, cause);
    }
}
