package com.learnhub.auth.service;

import com.learnhub.auth.dto.AuthResponse;
import com.learnhub.auth.dto.RegisterResponse;
import com.learnhub.auth.dto.TokenRefreshResponse;
import com.learnhub.auth.model.RefreshToken;
import com.learnhub.auth.repository.RefreshTokenRepository;
import com.learnhub.auth.validation.PasswordStrengthValidator;
import com.learnhub.user.model.EmailVerificationToken;
import com.learnhub.user.model.PasswordResetToken;
import com.learnhub.user.model.User;
import com.learnhub.notification.service.NotificationService;
import com.learnhub.user.repository.EmailVerificationTokenRepository;
import com.learnhub.user.repository.PasswordResetTokenRepository;
import com.learnhub.user.repository.UserRepository;
import com.learnhub.user.service.AccountLockoutService;
import com.learnhub.user.service.EmailNotificationService;
import com.learnhub.common.util.TokenProvider;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Core authentication service handling registration, login, token lifecycle,
 * password reset, and session management.
 *
 * Security properties:
 * - BCrypt cost 13 (configured in SecurityConfig)
 * - Constant-time password comparison via PasswordEncoder.matches()
 * - No PII (email/name) in log statements — only userId is logged
 * - Refresh token rotation: old token revoked immediately on refresh
 * - Account lockout after configurable failed attempts
 */
@Service
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHashService tokenHashService;
    private final AccountLockoutService accountLockoutService;
    private final EmailNotificationService emailNotificationService;
    private final NotificationService notificationService;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordStrengthValidator passwordStrengthValidator;
    private final TokenProvider tokenProvider;

    @Value("${jwt.access-token-ttl-seconds:900}")
    private long accessTokenTtlSeconds;

    @Value("${app.token.verification.expiry-hours:24}")
    private long verificationTokenExpiryHours;

    @Value("${app.token.verification.expiry-minutes:10}")
    private long verificationOtpExpiryMinutes;

    @Value("${app.token.verification.max-attempts:5}")
    private int verificationOtpMaxAttempts;

    @Value("${app.token.reset.expiry-hours:1}")
    private long resetTokenExpiryHours;

    public AuthService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        RefreshTokenRepository refreshTokenRepository,
        TokenHashService tokenHashService,
        AccountLockoutService accountLockoutService,
        EmailNotificationService emailNotificationService,
        NotificationService notificationService,
        EmailVerificationTokenRepository emailVerificationTokenRepository,
        PasswordResetTokenRepository passwordResetTokenRepository,
        PasswordStrengthValidator passwordStrengthValidator,
        TokenProvider tokenProvider
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenHashService = tokenHashService;
        this.accountLockoutService = accountLockoutService;
        this.emailNotificationService = emailNotificationService;
        this.notificationService = notificationService;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordStrengthValidator = passwordStrengthValidator;
        this.tokenProvider = tokenProvider;
    }

    // =========================================================================
    // ENDPOINT 1: Registration
    // =========================================================================

    /**
     * Register a new user with email/password.
     * Sends verification email asynchronously after user creation.
     *
     * @throws IllegalArgumentException if email already registered or password weak
     */
    @Transactional
    public RegisterResponse register(String email, String password, String firstName, String lastName) {
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (user.isEmailVerified()) {
                log.warn("Registration attempt for duplicate verified email (userId omitted)");
                throw new EmailAlreadyRegisteredException("Email is already registered");
            }

            updatePendingRegistration(user, password, firstName, lastName);
            queueVerificationOtp(user);
            log.info("Pending registration refreshed for userId: {}", user.getId());
            return new RegisterResponse(user.getId(), user.getEmail(), user.getRole());
        }

        // Password strength
        PasswordStrengthValidator.ValidationResult strength = passwordStrengthValidator.validate(password);
        if (!strength.valid()) {
            throw new WeakPasswordException(strength.message());
        }

        // Build and persist user
        User user = new User();
        user.setEmail(email);
        user.setUsername(email); // username = email for email/password flow
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole("STUDENT");
        user.setEmailVerified(false);

        User saved = userRepository.save(user);
        log.info("User registered: {}", saved.getId());

        queueVerificationOtp(saved);

        return new RegisterResponse(saved.getId(), saved.getEmail(), saved.getRole());
    }

    private void updatePendingRegistration(User user, String password, String firstName, String lastName) {
        PasswordStrengthValidator.ValidationResult strength = passwordStrengthValidator.validate(password);
        if (!strength.valid()) {
            throw new WeakPasswordException(strength.message());
        }

        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        userRepository.save(user);
    }

    private void queueVerificationOtp(User user) {
        try {
            String otp = tokenProvider.generateOtpCode();
            String tokenHash = tokenProvider.hashToken(otp);

            EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByUserId(user.getId())
                .orElseGet(() -> EmailVerificationToken.builder().user(user).build());
            // Keep legacy NOT NULL/UNIQUE token column populated without storing the raw OTP.
            verificationToken.setToken(tokenHash);
            verificationToken.setTokenHash(tokenHash);
            verificationToken.setExpiresAt(LocalDateTime.now().plusMinutes(verificationOtpExpiryMinutes));
            verificationToken.setVerifiedAt(null);
            verificationToken.setIsExpired(false);
            verificationToken.setAttemptCount(0);

            emailVerificationTokenRepository.save(verificationToken);
            notificationService.queueVerificationOtpEmail(user, otp);
            log.info("Verification OTP notification queued for userId: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to queue verification OTP notification for userId: {}", user.getId(), e);
        }
    }

    // =========================================================================
    // ENDPOINT 2: Email Verification
    // =========================================================================

    /**
     * Verify a user's email using the provided token.
     *
     * @param token the verification token
     * @throws com.learnhub.user.exception.InvalidTokenException if token invalid/expired
     */
    @Transactional
    public void verifyEmail(String token) {
        String tokenHash = tokenProvider.hashToken(token);

        EmailVerificationToken verificationToken = emailVerificationTokenRepository
            .findByTokenHash(tokenHash)
            .orElseThrow(() -> new com.learnhub.user.exception.InvalidTokenException("Invalid or expired verification token"));

        if (!verificationToken.isValid()) {
            throw new com.learnhub.user.exception.InvalidTokenException("Verification token has expired or been used");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);

        verificationToken.setVerifiedAt(LocalDateTime.now());
        verificationToken.setIsExpired(true);
        emailVerificationTokenRepository.save(verificationToken);

        log.info("Email verified for userId: {}", user.getId());
    }

    /**
     * Verify a user's email using the 6-digit OTP sent during registration.
     */
    @Transactional
    public void verifyEmailOtp(String email, String otp) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new com.learnhub.user.exception.InvalidTokenException("Invalid or expired verification code"));

        if (user.isEmailVerified()) {
            return;
        }

        EmailVerificationToken verificationToken = emailVerificationTokenRepository
            .findByUserId(user.getId())
            .orElseThrow(() -> new com.learnhub.user.exception.InvalidTokenException("Invalid or expired verification code"));

        if (!verificationToken.isValid()) {
            throw new com.learnhub.user.exception.InvalidTokenException("Verification code has expired. Please request a new code.");
        }

        if (verificationToken.getAttemptCount() != null
            && verificationToken.getAttemptCount() >= verificationOtpMaxAttempts) {
            verificationToken.setIsExpired(true);
            emailVerificationTokenRepository.save(verificationToken);
            throw new com.learnhub.user.exception.InvalidTokenException("Too many invalid attempts. Please request a new code.");
        }

        String otpHash = tokenProvider.hashToken(otp);
        if (!otpHash.equals(verificationToken.getTokenHash())) {
            int attempts = verificationToken.getAttemptCount() == null ? 0 : verificationToken.getAttemptCount();
            verificationToken.setAttemptCount(attempts + 1);
            emailVerificationTokenRepository.save(verificationToken);
            throw new com.learnhub.user.exception.InvalidTokenException("Invalid verification code");
        }

        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);

        verificationToken.setVerifiedAt(LocalDateTime.now());
        verificationToken.setIsExpired(true);
        emailVerificationTokenRepository.save(verificationToken);

        log.info("Email verified via OTP for userId: {}", user.getId());
    }

    /**
     * Issue a fresh activation OTP for an existing unverified account.
     */
    @Transactional
    public void resendVerificationOtp(String email) {
        Optional<User> maybeUser = userRepository.findByEmail(email);
        if (maybeUser.isEmpty()) {
            log.debug("Verification OTP resend requested for unknown email (omitted)");
            return;
        }

        User user = maybeUser.get();
        if (user.isEmailVerified()) {
            return;
        }

        queueVerificationOtp(user);
    }

    // =========================================================================
    // ENDPOINT 3: Login
    // =========================================================================

    /**
     * Authenticate a user and issue JWT + refresh token.
     *
     * Checks:
     * 1. User exists
     * 2. Email is verified
     * 3. Account is not locked
     * 4. Password matches
     *
     * On success: resets failed attempts counter, updates lastLogin.
     * On failure: records failed attempt (may trigger lockout).
     *
     * @throws IllegalArgumentException if credentials invalid
     * @throws AccountLockedException if account is locked
     */
    @Transactional
    public AuthResponse login(String email, String password) throws Exception {
        Optional<User> ou = userRepository.findByEmail(email);

        if (ou.isEmpty()) {
            // Don't reveal whether email exists — use generic message
            log.warn("Login attempt for unknown email (hash omitted for security)");
            throw new InvalidCredentialsException("Invalid credentials");
        }

        User user = ou.get();

        // Check email verification
        if (!user.isEmailVerified()) {
            log.warn("Login attempt for unverified userId: {}", user.getId());
            throw new EmailNotVerifiedException("Email address has not been verified. Please check your inbox.");
        }

        // Check account lockout BEFORE verifying password (prevents timing oracle)
        accountLockoutService.checkAndThrowIfLocked(user.getId());

        // Constant-time password comparison
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            accountLockoutService.recordFailedLoginAttempt(user.getId());
            log.warn("Failed login for userId: {}", user.getId());
            throw new InvalidCredentialsException("Invalid credentials");
        }

        // Success — reset failed attempts and update lastLogin
        accountLockoutService.recordSuccessfulLogin(user.getId());
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // Issue tokens
        String accessToken = jwtService.createAccessToken(user.getId().toString(), user.getRole());
        String refreshToken = jwtService.createRefreshToken(user.getId().toString());

        persistRefreshToken(user.getId(), refreshToken);

        log.info("Successful login for userId: {}", user.getId());
        return new AuthResponse(
            user.getId(),
            user.getRole(),
            user.getEmail(),
            accessToken,
            refreshToken,
            accessTokenTtlSeconds
        );
    }

    // =========================================================================
    // ENDPOINT 4: Logout
    // =========================================================================

    /**
     * Revoke the user's current refresh token.
     * Idempotent — no error if token not found.
     *
     * @param refreshToken the raw refresh token from Authorization header
     */
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return; // idempotent
        }
        try {
            if (!jwtService.validateToken(refreshToken)) {
                return; // idempotent — already invalid
            }
            SignedJWT jwt = SignedJWT.parse(refreshToken);
            String userId = jwt.getJWTClaimsSet().getSubject();
            String hash = tokenHashService.hashToken(refreshToken);
            refreshTokenRepository.findByTokenHash(hash)
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                    log.info("Refresh token revoked for userId: {}", userId);
                });
        } catch (Exception e) {
            log.debug("Logout called with unparseable token — treating as no-op");
        }
    }

    // =========================================================================
    // ENDPOINT 5: Forgot Password
    // =========================================================================

    /**
     * Initiate password reset. Always returns success to avoid email enumeration.
     *
     * @param email the email address to reset (may not exist — still returns OK)
     */
    @Transactional
    public void forgotPassword(String email) {
        Optional<User> ou = userRepository.findByEmail(email);
        if (ou.isEmpty()) {
            // Security: don't reveal that email is not registered
            log.debug("Forgot-password requested for unknown email (omitted)");
            return;
        }

        User user = ou.get();
        String token = tokenProvider.generateResetToken(user.getId());
        String tokenHash = tokenProvider.hashToken(token);

        PasswordResetToken resetToken = PasswordResetToken.builder()
            .user(user)
            .token(token)
            .tokenHash(tokenHash)
            .expiresAt(LocalDateTime.now().plusHours(resetTokenExpiryHours))
            .isUsed(false)
            .build();

        passwordResetTokenRepository.save(resetToken);
        emailNotificationService.sendPasswordResetEmail(user, token);
        log.info("Password reset email sent to userId: {}", user.getId());
    }

    // =========================================================================
    // ENDPOINT 6: Reset Password
    // =========================================================================

    /**
     * Complete a password reset using the provided token and new password.
     * Revokes ALL refresh tokens for the user after reset.
     *
     * @param token       the reset token
     * @param newPassword the new password
     * @throws com.learnhub.user.exception.InvalidTokenException if token invalid/expired
     * @throws WeakPasswordException if new password is too weak
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        // Validate password strength first
        PasswordStrengthValidator.ValidationResult strength = passwordStrengthValidator.validate(newPassword);
        if (!strength.valid()) {
            throw new WeakPasswordException(strength.message());
        }

        String tokenHash = tokenProvider.hashToken(token);
        PasswordResetToken resetToken = passwordResetTokenRepository
            .findByTokenHash(tokenHash)
            .orElseThrow(() -> new com.learnhub.user.exception.InvalidTokenException("Invalid or expired reset token"));

        if (!resetToken.isValid()) {
            throw new com.learnhub.user.exception.InvalidTokenException("Reset token has expired or been used");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setLastPasswordChange(LocalDateTime.now());
        user.setPasswordChangeRequired(false);
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsedAt(LocalDateTime.now());
        resetToken.setIsUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // Revoke ALL refresh tokens for this user
        int revoked = refreshTokenRepository.revokeAllByUserId(user.getId());
        log.info("Password reset: revoked {} refresh tokens for userId: {}", revoked, user.getId());
    }

    // =========================================================================
    // ENDPOINT 7: Token Refresh
    // =========================================================================

    /**
     * Rotate the refresh token: revoke old, issue new access + refresh pair.
     *
     * CRITICAL: old refresh token is revoked immediately before new tokens are issued.
     * Token reuse detection: if hash mismatch detected, all tokens for user are revoked.
     *
     * @param refreshToken the current refresh token
     * @return new access token, new refresh token, and expiry
     * @throws IllegalArgumentException if token invalid/expired/revoked
     */
    @Transactional
    public TokenRefreshResponse refresh(String refreshToken) throws Exception {
        if (!jwtService.validateToken(refreshToken)) {
            throw new InvalidCredentialsException("Invalid or expired refresh token");
        }

        SignedJWT jwt = SignedJWT.parse(refreshToken);
        String userId = jwt.getJWTClaimsSet().getSubject();
        String incomingHash = tokenHashService.hashToken(refreshToken);

        Optional<RefreshToken> rtOpt = refreshTokenRepository.findByUserIdAndRevokedFalse(
            UUID.fromString(userId));

        if (rtOpt.isEmpty()) {
            throw new InvalidCredentialsException("Refresh token not found or already revoked");
        }

        RefreshToken storedRt = rtOpt.get();
        if (!tokenHashService.verifyToken(refreshToken, storedRt.getTokenHash())) {
            // Token reuse detected — revoke all tokens for this user
            refreshTokenRepository.revokeAllByUserId(UUID.fromString(userId));
            log.warn("Refresh token reuse/mismatch detected for userId: {} — all sessions revoked", userId);
            throw new InvalidCredentialsException("Refresh token mismatch — possible replay attack. All sessions revoked.");
        }

        // Revoke old refresh token IMMEDIATELY (rotation)
        storedRt.setRevoked(true);
        refreshTokenRepository.save(storedRt);

        // Fetch user role
        String role = userRepository.findById(UUID.fromString(userId))
            .map(u -> u.getRole() != null ? u.getRole() : "LEARNER")
            .orElse("LEARNER");

        // Issue new tokens
        String newAccessToken = jwtService.createAccessToken(userId, role);
        String newRefreshToken = jwtService.createRefreshToken(userId);
        persistRefreshToken(UUID.fromString(userId), newRefreshToken);

        log.info("Refresh token rotated for userId: {}", userId);
        return new TokenRefreshResponse(newAccessToken, newRefreshToken, accessTokenTtlSeconds);
    }

    // =========================================================================
    // ENDPOINT 10: Revoke All Sessions (ADMIN)
    // =========================================================================

    /**
     * Revoke all active refresh tokens for a given user. Admin-only operation.
     *
     * @param userId the user whose sessions to revoke
     * @return count of revoked tokens
     */
    @Transactional
    public int revokeAllSessions(UUID userId) {
        int count = refreshTokenRepository.revokeAllByUserId(userId);
        log.info("Admin revoked {} sessions for userId: {}", count, userId);
        return count;
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    private void persistRefreshToken(UUID userId, String refreshToken) {
        String hash = tokenHashService.hashToken(refreshToken);
        RefreshToken rt = new RefreshToken();
        rt.setUserId(userId);
        rt.setTokenHash(hash);
        rt.setIssuedAt(Instant.now());
        rt.setExpiresAt(Instant.now().plusSeconds(2592000)); // 30 days
        rt.setRevoked(false);
        refreshTokenRepository.save(rt);
    }

    // =========================================================================
    // Custom exceptions (inner classes for locality)
    // =========================================================================

    public static class EmailAlreadyRegisteredException extends RuntimeException {
        public EmailAlreadyRegisteredException(String message) { super(message); }
    }

    public static class WeakPasswordException extends RuntimeException {
        public WeakPasswordException(String message) { super(message); }
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException(String message) { super(message); }
    }

    public static class EmailNotVerifiedException extends RuntimeException {
        public EmailNotVerifiedException(String message) { super(message); }
    }
}
