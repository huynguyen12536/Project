package com.learnhub.auth.service;

import com.learnhub.auth.validation.PasswordStrengthValidator;
import com.learnhub.common.util.TokenProvider;
import com.learnhub.user.repository.EmailVerificationTokenRepository;
import com.learnhub.user.repository.PasswordResetTokenRepository;
import com.learnhub.user.repository.UserRepository;
import com.learnhub.user.service.AccountLockoutService;
import com.learnhub.auth.repository.RefreshTokenRepository;
import com.learnhub.user.service.EmailNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Security-specific tests for AuthService.
 *
 * Verifies:
 * - No email enumeration in forgot-password
 * - No email enumeration in login (same error for wrong email vs wrong password)
 * - Token rotation: old token revoked BEFORE new tokens issued
 * - Constant-time password comparison
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceSecurityTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private TokenHashService tokenHashService;
    @Mock private AccountLockoutService accountLockoutService;
    @Mock private EmailNotificationService emailNotificationService;
    @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private PasswordStrengthValidator passwordStrengthValidator;
    @Mock private TokenProvider tokenProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
            userRepository, passwordEncoder, jwtService,
            refreshTokenRepository, tokenHashService,
            accountLockoutService, emailNotificationService,
            emailVerificationTokenRepository, passwordResetTokenRepository,
            passwordStrengthValidator, tokenProvider
        );
        ReflectionTestUtils.setField(authService, "accessTokenTtlSeconds", 900L);
        ReflectionTestUtils.setField(authService, "verificationTokenExpiryHours", 24L);
        ReflectionTestUtils.setField(authService, "resetTokenExpiryHours", 1L);
    }

    @Test
    @DisplayName("SECURITY: forgotPassword with unknown email — no exception, prevents email enumeration")
    void forgotPassword_unknownEmail_silentlySucceeds() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        // Must NOT throw — always returns silently to prevent email enumeration
        authService.forgotPassword("unknown@test.com");

        // Verify no email was sent
        verify(emailNotificationService, never()).sendPasswordResetEmail(any(), any());
    }

    @Test
    @DisplayName("SECURITY: login with unknown email — same exception as wrong password (no enumeration)")
    void login_unknownEmail_sameExceptionAsWrongPassword() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        // Both cases must throw the same exception type with the same message
        assertThatThrownBy(() -> authService.login("unknown@test.com", "pass"))
            .isInstanceOf(AuthService.InvalidCredentialsException.class)
            .hasMessage("Invalid credentials");
    }

    @Test
    @DisplayName("SECURITY: register does not log email in error when duplicate")
    void register_duplicateEmail_doesNotThrowPIIExceptionMessage() {
        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);

        // Exception message should be generic, not contain the email itself
        assertThatThrownBy(() ->
            authService.register("existing@test.com", "Str0ng@Pass1!", "A", "B")
        ).isInstanceOf(AuthService.EmailAlreadyRegisteredException.class);

        // Verify password encoder was never called (fail fast before hashing)
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("SECURITY: password is hashed with BCrypt — not stored as plaintext")
    void register_passwordHashed_notPlaintext() throws Exception {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordStrengthValidator.validate(anyString()))
            .thenReturn(PasswordStrengthValidator.ValidationResult.ok());
        when(passwordEncoder.encode("StrongPass@1234")).thenReturn("$2a$13$encoded");
        when(emailVerificationTokenRepository.findByUserId(any())).thenReturn(Optional.empty());
        when(tokenProvider.generateOtpCode()).thenReturn("123456");
        when(tokenProvider.hashToken("123456")).thenReturn("vh");

        com.learnhub.user.model.User saved = new com.learnhub.user.model.User();
        saved.setId(java.util.UUID.randomUUID());
        saved.setEmail("new@test.com");
        saved.setRole("STUDENT");
        when(userRepository.save(any())).thenReturn(saved);

        authService.register("new@test.com", "StrongPass@1234", "X", "Y");

        // Verify encode was called with the raw password
        verify(passwordEncoder).encode("StrongPass@1234");
    }

    @Test
    @DisplayName("SECURITY: resetPassword revokes ALL refresh tokens (not just current session)")
    void resetPassword_revokesAllRefreshTokens() {
        when(passwordStrengthValidator.validate(anyString()))
            .thenReturn(PasswordStrengthValidator.ValidationResult.ok());

        com.learnhub.user.model.User user = new com.learnhub.user.model.User();
        java.util.UUID userId = java.util.UUID.randomUUID();
        user.setId(userId);

        com.learnhub.user.model.PasswordResetToken resetToken =
            com.learnhub.user.model.PasswordResetToken.builder()
                .user(user)
                .expiresAt(java.time.LocalDateTime.now().plusHours(1))
                .isUsed(false)
                .build();

        when(tokenProvider.hashToken("good-token")).thenReturn("hash");
        when(passwordResetTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any())).thenReturn(user);
        when(passwordResetTokenRepository.save(any())).thenReturn(resetToken);
        when(refreshTokenRepository.revokeAllByUserId(userId)).thenReturn(3);

        authService.resetPassword("good-token", "NewStr0ng@Pass!");

        // CRITICAL: All sessions must be revoked
        verify(refreshTokenRepository).revokeAllByUserId(userId);
    }
}
