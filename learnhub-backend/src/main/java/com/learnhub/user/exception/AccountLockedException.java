package com.learnhub.user.exception;

public class AccountLockedException extends RuntimeException {
    private Long remainingMinutes;

    public AccountLockedException(String message) {
        super(message);
    }

    public AccountLockedException(String message, Long remainingMinutes) {
        super(message);
        this.remainingMinutes = remainingMinutes;
    }

    public Long getRemainingMinutes() {
        return remainingMinutes;
    }
}
