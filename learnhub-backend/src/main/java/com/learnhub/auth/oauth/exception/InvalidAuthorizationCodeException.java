package com.learnhub.auth.oauth.exception;

/**
 * Exception for invalid GitHub authorization codes.
 * Thrown when code-to-token exchange fails or code is invalid.
 */
public class InvalidAuthorizationCodeException extends GitHubOAuthException {
    public InvalidAuthorizationCodeException(String message) {
        super(message);
    }

    public InvalidAuthorizationCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
