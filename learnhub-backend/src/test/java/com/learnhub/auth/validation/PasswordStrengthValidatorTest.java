package com.learnhub.auth.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PasswordStrengthValidator.
 * Covers all validation rules: length, upper, lower, digit, special char.
 */
class PasswordStrengthValidatorTest {

    private PasswordStrengthValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PasswordStrengthValidator();
    }

    @Test
    @DisplayName("Strong password passes all checks")
    void strongPassword_passes() {
        assertThat(validator.validate("Str0ng@Password!").valid()).isTrue();
    }

    @ParameterizedTest
    @DisplayName("Too short passwords fail")
    @ValueSource(strings = {"Short@1", "Abc!9x", "Abcdef@123"})
    void tooShortPassword_fails(String password) {
        PasswordStrengthValidator.ValidationResult result = validator.validate(password);
        assertThat(result.valid()).isFalse();
        assertThat(result.message()).contains("12 characters");
    }

    @Test
    @DisplayName("Password without uppercase fails")
    void noUppercase_fails() {
        PasswordStrengthValidator.ValidationResult result = validator.validate("allowercase12@pass");
        assertThat(result.valid()).isFalse();
        assertThat(result.message()).containsIgnoringCase("uppercase");
    }

    @Test
    @DisplayName("Password without lowercase fails")
    void noLowercase_fails() {
        PasswordStrengthValidator.ValidationResult result = validator.validate("ALLUPPERCASE12@PASS");
        assertThat(result.valid()).isFalse();
        assertThat(result.message()).containsIgnoringCase("lowercase");
    }

    @Test
    @DisplayName("Password without digit fails")
    void noDigit_fails() {
        PasswordStrengthValidator.ValidationResult result = validator.validate("NoDigitsHere@Pass");
        assertThat(result.valid()).isFalse();
        assertThat(result.message()).containsIgnoringCase("number");
    }

    @Test
    @DisplayName("Password without special character fails")
    void noSpecialChar_fails() {
        PasswordStrengthValidator.ValidationResult result = validator.validate("NoSpecialChar1Password");
        assertThat(result.valid()).isFalse();
        assertThat(result.message()).containsIgnoringCase("special");
    }

    @Test
    @DisplayName("Null password fails with length message")
    void nullPassword_fails() {
        PasswordStrengthValidator.ValidationResult result = validator.validate(null);
        assertThat(result.valid()).isFalse();
    }

    @Test
    @DisplayName("Exactly 12 chars with all requirements passes")
    void exactly12Chars_withAllRequirements_passes() {
        // 12 chars: upper + lower + digit + special
        assertThat(validator.validate("Abcdef@12345").valid()).isTrue();
    }
}
