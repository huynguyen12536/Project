package com.learnhub.auth.oauth.exception;

/**
 * Thrown when attempting to use GitHub OAuth connection that doesn't exist.
 * Returns HTTP 404 Not Found.
 */
public class OAuthNotConnectedException extends GitHubOAuthException {

    public OAuthNotConnectedException(String message) {
        super(message);
    }

    public OAuthNotConnectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
