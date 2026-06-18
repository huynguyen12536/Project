package com.learnhub.github.exception;

/**
 * Exception thrown when attempting to select a repository that is already selected by the user.
 */
public class RepositoryAlreadySelectedException extends RuntimeException {
    public RepositoryAlreadySelectedException(String message) {
        super(message);
    }

    public RepositoryAlreadySelectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
