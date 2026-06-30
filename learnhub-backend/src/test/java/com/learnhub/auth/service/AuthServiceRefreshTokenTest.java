package com.learnhub.auth.service;

import com.learnhub.auth.dto.TokenRefreshResponse;
import com.learnhub.auth.model.RefreshToken;
import com.learnhub.auth.repository.RefreshTokenRepository;
import com.learnhub.auth.validation.PasswordStrengthValidator;
import com.learnhub.common.util.TokenProvider;
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
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Security unit tests for AuthService refresh token rotation.
 *
 * BLOCKER 3 verification: Ensures that:
 * 1. Each refresh call issues a NEW refresh token (never echoes the old one back)
 * 2. The old refresh token is revoked IMMEDIATELY upon use
 * 3. Attempting to reuse a revoked (old) refresh token results in an exception (401)
 * 4. accessToken != refreshToken in the response
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceRefreshTokenTest {

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

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String USER_ID_STR = USER_ID.toString();

    // Real RSA key pair for generating test JWTs
    private RSAPrivateKey rsaPrivateKey;
    private RSAPublicKey rsaPublicKey;

    private String validRefreshToken;
    private static final String OLD_TOKEN_HASH = "stored-bcrypt-hash-of-old-token";
    private static final String NEW_ACCESS_TOKEN = "new.access.token";
    private static final String NEW_REFRESH_TOKEN = "new.refresh.token";
    private static final String NEW_TOKEN_HASH = "new-token-hash";

    @BeforeEach
    void setUp() throws Exception {
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

        // Generate a real RSA key pair for signing test JWTs
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        rsaPrivateKey = (RSAPrivateKey) kp.getPrivate();
        rsaPublicKey = (RSAPublicKey) kp.getPublic();

        // Create a real, well-formed refresh JWT for the test user
        validRefreshToken = buildRealRefreshToken(USER_ID_STR);
    }

    /**
     * BLOCKER 3 (core): refresh() must return a NEW refresh token, never echo the submitted one.
     * The old broken code was: String refresh = access; — returning access token as refresh.
     */
    @Test
    void refresh_MustReturnNewRefreshToken_NotEchoOldOne() throws Exception {
        setupValidRefreshScenario();

        TokenRefreshResponse result = authService.refresh(validRefreshToken);

        String returnedRefreshToken = result.refreshToken();
        assertNotNull(returnedRefreshToken, "refreshToken must be present in response");
        assertNotEquals(validRefreshToken, returnedRefreshToken,
            "SECURITY: refresh() must return a NEW refresh token. " +
            "The old bug returned the same (or access) token as refresh."
        );
        assertEquals(NEW_REFRESH_TOKEN, returnedRefreshToken,
            "New refresh token from jwtService must be in response"
        );
    }

    /**
     * BLOCKER 3: accessToken and refreshToken in response must be different values.
     */
    @Test
    void refresh_AccessTokenAndRefreshTokenMustBeDifferent() throws Exception {
        setupValidRefreshScenario();

        TokenRefreshResponse result = authService.refresh(validRefreshToken);

        assertNotEquals(result.token(), result.refreshToken(),
            "SECURITY: accessToken and refreshToken must be distinct values in refresh response."
        );
    }

    /**
     * BLOCKER 3 (rotation): The old refresh token must be revoked immediately upon use.
     * Captures the save() calls to the repository to verify the old token is marked revoked.
     */
    @Test
    void refresh_OldTokenMustBeRevokedImmediately() throws Exception {
        setupValidRefreshScenario();

        authService.refresh(validRefreshToken);

        // Capture all save() calls
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, atLeast(1)).save(captor.capture());

        // First saved entity must be the old token marked revoked
        RefreshToken revokedToken = captor.getAllValues().get(0);
        assertTrue(Boolean.TRUE.equals(revokedToken.getRevoked()),
            "SECURITY: Old refresh token must be revoked=true immediately upon rotation."
        );
        assertEquals(OLD_TOKEN_HASH, revokedToken.getTokenHash(),
            "The revoked entity must match the original stored token hash."
        );
    }

    /**
     * BLOCKER 3 (reuse attack prevention): Using a revoked token must throw an exception.
     * At the HTTP layer this becomes a 401 Unauthorized response.
     */
    @Test
    void refresh_ReuseOfRevokedToken_MustThrowException() throws Exception {
        // Token passes JWT signature validation, but no active entry exists in DB (already revoked)
        when(jwtService.validateToken(validRefreshToken)).thenReturn(true);
        when(refreshTokenRepository.findByUserIdAndRevokedFalse(USER_ID))
            .thenReturn(Optional.empty()); // simulates revoked / not found

        AuthService.InvalidCredentialsException ex = assertThrows(
            AuthService.InvalidCredentialsException.class,
            () -> authService.refresh(validRefreshToken),
            "SECURITY: Reuse of a revoked refresh token must throw InvalidCredentialsException (→ 401)."
        );

        assertThat(ex.getMessage()).satisfiesAnyOf(
            msg -> assertThat(msg).containsIgnoringCase("revoked"),
            msg -> assertThat(msg).containsIgnoringCase("not found")
        );
    }

    /**
     * Verifies response map contains both expected token keys with correct values.
     */
    @Test
    void refresh_ResponseContainsBothTokenKeys() throws Exception {
        setupValidRefreshScenario();

        TokenRefreshResponse result = authService.refresh(validRefreshToken);

        assertThat(result.token()).isEqualTo(NEW_ACCESS_TOKEN);
        assertThat(result.refreshToken()).isEqualTo(NEW_REFRESH_TOKEN);
    }

    /**
     * Verifies that a new RefreshToken record is persisted to the repository after rotation.
     */
    @Test
    void refresh_NewRefreshTokenMustBePersisted() throws Exception {
        setupValidRefreshScenario();

        authService.refresh(validRefreshToken);

        // Expect exactly 2 saves: old token revocation + new token persistence
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(2)).save(captor.capture());

        RefreshToken newToken = captor.getAllValues().get(1);
        assertFalse(Boolean.TRUE.equals(newToken.getRevoked()), "New refresh token must be stored with revoked=false");
        assertEquals(USER_ID, newToken.getUserId(), "New token must be associated with the correct userId");
        assertEquals(NEW_TOKEN_HASH, newToken.getTokenHash(), "New token must be stored with correct hash");
    }

    /**
     * Verifies that a hash-mismatch (possible replay attack) immediately revokes the stored token.
     */
    @Test
    void refresh_HashMismatch_RevokesStoredTokenAndThrows() throws Exception {
        when(jwtService.validateToken(validRefreshToken)).thenReturn(true);

        RefreshToken storedRt = buildStoredRefreshToken();
        when(refreshTokenRepository.findByUserIdAndRevokedFalse(USER_ID))
            .thenReturn(Optional.of(storedRt));

        // Token doesn't match the stored hash — simulate replay with tampered token
        when(tokenHashService.hashToken(validRefreshToken)).thenReturn("computed-hash-does-not-matter");
        when(tokenHashService.verifyToken(validRefreshToken, OLD_TOKEN_HASH)).thenReturn(false);

        when(refreshTokenRepository.revokeAllByUserId(USER_ID)).thenReturn(1);

        AuthService.InvalidCredentialsException ex = assertThrows(
            AuthService.InvalidCredentialsException.class,
            () -> authService.refresh(validRefreshToken),
            "Hash mismatch must be treated as a replay attack and must throw."
        );

        assertThat(ex.getMessage()).satisfiesAnyOf(
            msg -> assertThat(msg).containsIgnoringCase("mismatch"),
            msg -> assertThat(msg).containsIgnoringCase("replay")
        );

        // All sessions for user must be revoked on replay attack
        verify(refreshTokenRepository).revokeAllByUserId(USER_ID);
    }

    // --- Private helpers ---

    private String buildRealRefreshToken(String subject) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .claim("token_type", "refresh")
                .claim("token_version", 1)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(2592000)))
                .jwtID(UUID.randomUUID().toString())
                .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID("1")
                .build();

        SignedJWT jwt = new SignedJWT(header, claims);
        jwt.sign(new RSASSASigner(rsaPrivateKey));
        return jwt.serialize();
    }

    private RefreshToken buildStoredRefreshToken() {
        RefreshToken rt = new RefreshToken();
        rt.setUserId(USER_ID);
        rt.setTokenHash(OLD_TOKEN_HASH);
        rt.setIssuedAt(Instant.now().minusSeconds(60));
        rt.setExpiresAt(Instant.now().plusSeconds(2592000));
        rt.setRevoked(false);
        return rt;
    }

    private void setupValidRefreshScenario() throws Exception {
        when(jwtService.validateToken(validRefreshToken)).thenReturn(true);

        RefreshToken storedRt = buildStoredRefreshToken();
        when(refreshTokenRepository.findByUserIdAndRevokedFalse(USER_ID))
            .thenReturn(Optional.of(storedRt));

        when(tokenHashService.hashToken(validRefreshToken)).thenReturn("computed-incoming-hash");
        when(tokenHashService.verifyToken(validRefreshToken, OLD_TOKEN_HASH)).thenReturn(true);
        when(tokenHashService.hashToken(NEW_REFRESH_TOKEN)).thenReturn(NEW_TOKEN_HASH);

        User user = new User();
        user.setId(USER_ID);
        user.setRole("LEARNER");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        when(jwtService.createAccessToken(USER_ID_STR, "LEARNER")).thenReturn(NEW_ACCESS_TOKEN);
        when(jwtService.createRefreshToken(USER_ID_STR)).thenReturn(NEW_REFRESH_TOKEN);

        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));
    }
}
