package com.learnhub.auth.oauth.exception;

/**
 * Thrown when GitHub authorization code is invalid, expired, or cannot be exchanged.
 * Returns HTTP 400 Bad Request.
 */
public class InvalidAuthorizationCodeException extends GitHubOAuthException {

    public InvalidAuthorizationCodeException(String message) {
        super(message);
    }

    public InvalidAuthorizationCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
