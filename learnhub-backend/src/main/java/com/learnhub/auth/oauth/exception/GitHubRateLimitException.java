package com.learnhub.auth.oauth.exception;

public class GitHubRateLimitException extends GitHubOAuthException {
    private final long resetTimestamp;

    public GitHubRateLimitException(String message, long resetTimestamp) {
        super(message);
        this.resetTimestamp = resetTimestamp;
    }

    public long getResetTimestamp() {
        return resetTimestamp;
    }
}
