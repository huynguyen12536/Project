package com.learnhub.github.exception;

/**
 * Exception thrown when a snapshot cannot be found or is not owned by the user.
 */
public class SnapshotNotFoundException extends RuntimeException {
    public SnapshotNotFoundException(String message) {
        super(message);
    }

    public SnapshotNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
