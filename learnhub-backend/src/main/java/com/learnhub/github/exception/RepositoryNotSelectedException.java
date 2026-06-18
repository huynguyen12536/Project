package com.learnhub.github.exception;

/**
 * Exception thrown when attempting to access a repository that has not been selected by the user.
 */
public class RepositoryNotSelectedException extends RuntimeException {
    public RepositoryNotSelectedException(String message) {
        super(message);
    }

    public RepositoryNotSelectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
