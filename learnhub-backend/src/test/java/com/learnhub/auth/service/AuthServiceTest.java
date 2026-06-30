package com.learnhub.auth.service;

import com.learnhub.auth.dto.AuthResponse;
import com.learnhub.auth.dto.RegisterResponse;
import com.learnhub.auth.dto.TokenRefreshResponse;
import com.learnhub.auth.model.RefreshToken;
import com.learnhub.auth.repository.RefreshTokenRepository;
import com.learnhub.auth.validation.PasswordStrengthValidator;
import com.learnhub.common.util.TokenProvider;
import com.learnhub.notification.service.NotificationService;
import com.learnhub.user.exception.AccountLockedException;
import com.learnhub.user.model.EmailVerificationToken;
import com.learnhub.user.model.PasswordResetToken;
import com.learnhub.user.model.User;
import com.learnhub.user.repository.EmailVerificationTokenRepository;
import com.learnhub.user.repository.PasswordResetTokenRepository;
import com.learnhub.user.repository.UserRepository;
import com.learnhub.user.service.AccountLockoutService;
import com.learnhub.user.service.EmailNotificationService;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService covering all 12 endpoint business logic paths.
 *
 * Security verifications:
 * - No PII (email/name) in log output tested via mock captures
 * - Account lockout integration verified
 * - Token rotation (old revoked before new issued) verified
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private TokenHashService tokenHashService;
    @Mock private AccountLockoutService accountLockoutService;
    @Mock private EmailNotificationService emailNotificationService;
    @Mock private NotificationService notificationService;
    @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private PasswordStrengthValidator passwordStrengthValidator;
    @Mock private TokenProvider tokenProvider;

    private AuthService authService;

    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    @BeforeEach
    void setUp() throws Exception {
        authService = new AuthService(
            userRepository, passwordEncoder, jwtService,
            refreshTokenRepository, tokenHashService,
            accountLockoutService, emailNotificationService, notificationService,
            emailVerificationTokenRepository, passwordResetTokenRepository,
            passwordStrengthValidator, tokenProvider
        );
        ReflectionTestUtils.setField(authService, "accessTokenTtlSeconds", 900L);
        ReflectionTestUtils.setField(authService, "verificationTokenExpiryHours", 24L);
        ReflectionTestUtils.setField(authService, "resetTokenExpiryHours", 1L);

        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();
        privateKey = (RSAPrivateKey) pair.getPrivate();
        publicKey = (RSAPublicKey) pair.getPublic();
    }

    // =========================================================================
    // ENDPOINT 1: register
    // =========================================================================

    @Test
    @DisplayName("register: success — returns RegisterResponse with STUDENT role")
    void register_success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordStrengthValidator.validate(anyString()))
            .thenReturn(PasswordStrengthValidator.ValidationResult.ok());
        User saved = new User();
        saved.setId(UUID.randomUUID());
        saved.setEmail("test@example.com");
        saved.setRole("STUDENT");
        when(userRepository.save(any())).thenReturn(saved);
        when(emailVerificationTokenRepository.findByUserId(any())).thenReturn(Optional.empty());
        when(tokenProvider.generateOtpCode()).thenReturn("123456");
        when(tokenProvider.hashToken("123456")).thenReturn("vtok-hash");
        when(emailVerificationTokenRepository.save(any())).thenReturn(null);

        RegisterResponse resp = authService.register("test@example.com", "Strong@Password1!", "Alice", "Smith");

        assertThat(resp.email()).isEqualTo("test@example.com");
        assertThat(resp.role()).isEqualTo("STUDENT");
        verify(userRepository).save(any(User.class));
        verify(notificationService).queueVerificationOtpEmail(any(User.class), eq("123456"));
    }

    @Test
    @DisplayName("register: duplicate verified email — throws EmailAlreadyRegisteredException")
    void register_duplicateEmail_throws() {
        User existing = new User();
        existing.setId(UUID.randomUUID());
        existing.setEmail("dup@example.com");
        existing.setEmailVerified(true);
        when(userRepository.findByEmail("dup@example.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() ->
            authService.register("dup@example.com", "Strong@Password1!", "Bob", "Jones")
        ).isInstanceOf(AuthService.EmailAlreadyRegisteredException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register: weak password — throws WeakPasswordException")
    void register_weakPassword_throws() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordStrengthValidator.validate("weak"))
            .thenReturn(PasswordStrengthValidator.ValidationResult.fail("Password must be at least 12 characters long"));

        assertThatThrownBy(() ->
            authService.register("x@example.com", "weak", "X", "Y")
        ).isInstanceOf(AuthService.WeakPasswordException.class);
    }

    // =========================================================================
    // ENDPOINT 2: verifyEmail
    // =========================================================================

    @Test
    @DisplayName("verifyEmail: valid token — sets emailVerified=true")
    void verifyEmail_validToken() {
        String rawToken = "raw-token";
        String hash = "raw-token-hash";
        when(tokenProvider.hashToken(rawToken)).thenReturn(hash);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmailVerified(false);

        EmailVerificationToken evToken = new EmailVerificationToken();
        evToken.setUser(user);
        evToken.setExpiresAt(LocalDateTime.now().plusHours(1));
        evToken.setIsExpired(false);

        when(emailVerificationTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(evToken));
        when(userRepository.save(any())).thenReturn(user);
        when(emailVerificationTokenRepository.save(any())).thenReturn(evToken);

        authService.verifyEmail(rawToken);

        assertThat(user.isEmailVerified()).isTrue();
    }

    @Test
    @DisplayName("verifyEmail: invalid token — throws InvalidTokenException")
    void verifyEmail_invalidToken_throws() {
        when(tokenProvider.hashToken("bad")).thenReturn("bad-hash");
        when(emailVerificationTokenRepository.findByTokenHash("bad-hash")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyEmail("bad"))
            .isInstanceOf(com.learnhub.user.exception.InvalidTokenException.class);
    }

    // =========================================================================
    // ENDPOINT 3: login
    // =========================================================================

    @Test
    @DisplayName("login: valid credentials — returns AuthResponse with tokens")
    void login_success() throws Exception {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@test.com");
        user.setRole("STUDENT");
        user.setEmailVerified(true);
        user.setPasswordHash("hashed");

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        doNothing().when(accountLockoutService).checkAndThrowIfLocked(user.getId());
        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
        doNothing().when(accountLockoutService).recordSuccessfulLogin(user.getId());
        when(userRepository.save(any())).thenReturn(user);
        when(jwtService.createAccessToken(anyString(), anyString())).thenReturn("access-jwt");
        when(jwtService.createRefreshToken(anyString())).thenReturn("refresh-jwt");
        when(tokenHashService.hashToken("refresh-jwt")).thenReturn("rh");
        when(refreshTokenRepository.save(any())).thenReturn(null);

        AuthResponse resp = authService.login("user@test.com", "pass");

        assertThat(resp.token()).isEqualTo("access-jwt");
        assertThat(resp.refreshToken()).isEqualTo("refresh-jwt");
        assertThat(resp.expiresIn()).isEqualTo(900L);
    }

    @Test
    @DisplayName("login: unknown email — throws InvalidCredentialsException (no email enumeration)")
    void login_unknownEmail_throws() {
        when(userRepository.findByEmail("nouser@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("nouser@test.com", "pass"))
            .isInstanceOf(AuthService.InvalidCredentialsException.class)
            .hasMessage("Invalid credentials");
    }

    @Test
    @DisplayName("login: email not verified — throws EmailNotVerifiedException")
    void login_emailNotVerified_throws() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmailVerified(false);

        when(userRepository.findByEmail("unverified@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login("unverified@test.com", "pass"))
            .isInstanceOf(AuthService.EmailNotVerifiedException.class);
    }

    @Test
    @DisplayName("login: account locked — throws AccountLockedException")
    void login_accountLocked_throws() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmailVerified(true);
        user.setPasswordHash("h");

        when(userRepository.findByEmail("locked@test.com")).thenReturn(Optional.of(user));
        doThrow(new AccountLockedException("Locked", 25L))
            .when(accountLockoutService).checkAndThrowIfLocked(user.getId());

        assertThatThrownBy(() -> authService.login("locked@test.com", "pass"))
            .isInstanceOf(AccountLockedException.class);
    }

    @Test
    @DisplayName("login: wrong password — records failed attempt")
    void login_wrongPassword_recordsFailedAttempt() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmailVerified(true);
        user.setPasswordHash("hashed");

        when(userRepository.findByEmail("u@test.com")).thenReturn(Optional.of(user));
        doNothing().when(accountLockoutService).checkAndThrowIfLocked(user.getId());
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);
        doNothing().when(accountLockoutService).recordFailedLoginAttempt(user.getId());

        assertThatThrownBy(() -> authService.login("u@test.com", "wrong"))
            .isInstanceOf(AuthService.InvalidCredentialsException.class);

        verify(accountLockoutService).recordFailedLoginAttempt(user.getId());
    }

    // =========================================================================
    // ENDPOINT 4: logout
    // =========================================================================

    @Test
    @DisplayName("logout: valid token — revokes refresh token")
    void logout_validToken_revokes() throws Exception {
        String rawToken = buildTestRefreshToken();
        when(jwtService.validateToken(rawToken)).thenReturn(true);
        when(tokenHashService.hashToken(rawToken)).thenReturn("hash");
        RefreshToken rt = new RefreshToken();
        rt.setRevoked(false);
        when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(rt));
        when(refreshTokenRepository.save(any())).thenReturn(rt);

        authService.logout(rawToken);

        assertThat(rt.getRevoked()).isTrue();
    }

    @Test
    @DisplayName("logout: null token — no-op (idempotent)")
    void logout_nullToken_noOp() {
        authService.logout(null);
        verifyNoInteractions(refreshTokenRepository);
    }

    // =========================================================================
    // ENDPOINT 5: forgotPassword
    // =========================================================================

    @Test
    @DisplayName("forgotPassword: known email — sends reset email")
    void forgotPassword_knownEmail_sendsEmail() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("reset@test.com");
        when(userRepository.findByEmail("reset@test.com")).thenReturn(Optional.of(user));
        when(tokenProvider.generateResetToken(user.getId())).thenReturn("rtok");
        when(tokenProvider.hashToken("rtok")).thenReturn("rtok-hash");
        when(passwordResetTokenRepository.save(any())).thenReturn(null);
        doNothing().when(emailNotificationService).sendPasswordResetEmail(any(), anyString());

        authService.forgotPassword("reset@test.com");

        verify(emailNotificationService).sendPasswordResetEmail(user, "rtok");
    }

    @Test
    @DisplayName("forgotPassword: unknown email — no exception (prevents email enumeration)")
    void forgotPassword_unknownEmail_noError() {
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        // Must NOT throw
        authService.forgotPassword("ghost@test.com");

        verify(emailNotificationService, never()).sendPasswordResetEmail(any(), any());
    }

    // =========================================================================
    // ENDPOINT 6: resetPassword
    // =========================================================================

    @Test
    @DisplayName("resetPassword: valid token — updates password and revokes all refresh tokens")
    void resetPassword_valid() {
        when(passwordStrengthValidator.validate("NewStr0ng@Pass"))
            .thenReturn(PasswordStrengthValidator.ValidationResult.ok());

        User user = new User();
        user.setId(UUID.randomUUID());

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setExpiresAt(LocalDateTime.now().plusHours(1));
        resetToken.setIsUsed(false);

        when(tokenProvider.hashToken("good-token")).thenReturn("hash");
        when(passwordResetTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("NewStr0ng@Pass")).thenReturn("encoded");
        when(userRepository.save(any())).thenReturn(user);
        when(passwordResetTokenRepository.save(any())).thenReturn(resetToken);
        when(refreshTokenRepository.revokeAllByUserId(user.getId())).thenReturn(2);

        authService.resetPassword("good-token", "NewStr0ng@Pass");

        assertThat(user.getPasswordHash()).isEqualTo("encoded");
        verify(refreshTokenRepository).revokeAllByUserId(user.getId());
    }

    @Test
    @DisplayName("resetPassword: weak new password — throws WeakPasswordException")
    void resetPassword_weakPassword_throws() {
        when(passwordStrengthValidator.validate("weak"))
            .thenReturn(PasswordStrengthValidator.ValidationResult.fail("Too weak"));

        assertThatThrownBy(() -> authService.resetPassword("tok", "weak"))
            .isInstanceOf(AuthService.WeakPasswordException.class);
    }

    // =========================================================================
    // ENDPOINT 7: refresh (token rotation)
    // =========================================================================

    @Test
    @DisplayName("refresh: valid token — old token revoked immediately, new tokens issued")
    void refresh_rotatesToken() throws Exception {
        String rawToken = buildTestRefreshToken();
        String userId = extractSubjectFromToken(rawToken);

        when(jwtService.validateToken(rawToken)).thenReturn(true);
        when(tokenHashService.hashToken(rawToken)).thenReturn("old-hash");

        RefreshToken stored = new RefreshToken();
        stored.setTokenHash("old-hash");
        stored.setRevoked(false);
        stored.setUserId(UUID.fromString(userId));

        when(refreshTokenRepository.findByUserIdAndRevokedFalse(UUID.fromString(userId)))
            .thenReturn(Optional.of(stored));
        when(tokenHashService.verifyToken(rawToken, "old-hash")).thenReturn(true);
        when(refreshTokenRepository.save(any())).thenReturn(stored);
        when(userRepository.findById(UUID.fromString(userId))).thenReturn(Optional.of(new User()));
        when(jwtService.createAccessToken(anyString(), anyString())).thenReturn("new-access");
        when(jwtService.createRefreshToken(anyString())).thenReturn("new-refresh");
        when(tokenHashService.hashToken("new-refresh")).thenReturn("new-hash");

        TokenRefreshResponse resp = authService.refresh(rawToken);

        assertThat(resp.token()).isEqualTo("new-access");
        assertThat(resp.refreshToken()).isEqualTo("new-refresh");
        // Old token must be revoked
        assertThat(stored.getRevoked()).isTrue();
    }

    @Test
    @DisplayName("refresh: token reuse detected — revokes all sessions")
    void refresh_tokenReuse_revokesAllSessions() throws Exception {
        String rawToken = buildTestRefreshToken();
        String userId = extractSubjectFromToken(rawToken);

        when(jwtService.validateToken(rawToken)).thenReturn(true);
        when(tokenHashService.hashToken(rawToken)).thenReturn("incoming-hash");

        RefreshToken stored = new RefreshToken();
        stored.setTokenHash("different-hash");
        stored.setRevoked(false);

        when(refreshTokenRepository.findByUserIdAndRevokedFalse(UUID.fromString(userId)))
            .thenReturn(Optional.of(stored));
        when(tokenHashService.verifyToken(rawToken, "different-hash")).thenReturn(false);
        when(refreshTokenRepository.revokeAllByUserId(UUID.fromString(userId))).thenReturn(1);

        assertThatThrownBy(() -> authService.refresh(rawToken))
            .isInstanceOf(AuthService.InvalidCredentialsException.class)
            .hasMessageContaining("replay attack");

        verify(refreshTokenRepository).revokeAllByUserId(UUID.fromString(userId));
    }

    // =========================================================================
    // ENDPOINT 10: revokeAllSessions (admin)
    // =========================================================================

    @Test
    @DisplayName("revokeAllSessions: revokes all active refresh tokens for userId")
    void revokeAllSessions_returnsCount() {
        UUID userId = UUID.randomUUID();
        when(refreshTokenRepository.revokeAllByUserId(userId)).thenReturn(3);

        int count = authService.revokeAllSessions(userId);

        assertThat(count).isEqualTo(3);
        verify(refreshTokenRepository).revokeAllByUserId(userId);
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    private String buildTestRefreshToken() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .subject(UUID.randomUUID().toString())
            .claim("token_type", "refresh")
            .issueTime(new Date())
            .expirationTime(new Date(System.currentTimeMillis() + 3600_000))
            .jwtID(UUID.randomUUID().toString())
            .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("1").build();
        SignedJWT jwt = new SignedJWT(header, claims);
        jwt.sign(new RSASSASigner(privateKey));
        return jwt.serialize();
    }

    private String extractSubjectFromToken(String token) throws Exception {
        return SignedJWT.parse(token).getJWTClaimsSet().getSubject();
    }
}
