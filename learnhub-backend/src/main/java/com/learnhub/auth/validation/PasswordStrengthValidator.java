package com.learnhub.auth.validation;

import org.springframework.stereotype.Component;

/**
 * Validates password strength according to OWASP guidelines.
 * Requirements: min 12 chars, at least one uppercase, one lowercase, one digit, one special char.
 * No PII is logged in this component.
 */
@Component
public class PasswordStrengthValidator {

    private static final int MIN_LENGTH = 12;

    public ValidationResult validate(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            return ValidationResult.fail("Password must be at least 12 characters long");
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }

        if (!hasUpper) return ValidationResult.fail("Password must contain at least one uppercase letter");
        if (!hasLower) return ValidationResult.fail("Password must contain at least one lowercase letter");
        if (!hasDigit) return ValidationResult.fail("Password must contain at least one number");
        if (!hasSpecial) return ValidationResult.fail("Password must contain at least one special character");

        return ValidationResult.ok();
    }

    public record ValidationResult(boolean valid, String message) {
        public static ValidationResult ok() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult fail(String message) {
            return new ValidationResult(false, message);
        }
    }
}
