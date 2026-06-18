package com.learnhub.auth.oauth.exception;

/**
 * Thrown when OAuth state token doesn't match or is invalid/expired.
 * Returns HTTP 401 Unauthorized.
 */
public class StateTokenMismatchException extends GitHubOAuthException {

    public StateTokenMismatchException(String message) {
        super(message);
    }

    public StateTokenMismatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
