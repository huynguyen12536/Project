package com.learnhub.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnhub.auth.dto.LoginRequest;
import com.learnhub.auth.dto.RegisterRequest;
import com.learnhub.auth.dto.VerifyEmailRequest;
import com.learnhub.auth.filter.JwtAuthenticationFilter;
import com.learnhub.auth.service.AuthCookieService;
import com.learnhub.auth.service.AuthService;
import com.learnhub.auth.service.JwtService;
import com.learnhub.auth.service.RateLimitingService;
import com.learnhub.auth.service.RsaKeyManager;
import com.learnhub.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseCookie;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration-style tests for AuthController using MockMvc (no database).
 *
 * Verifies:
 * - HTTP status codes for all endpoints
 * - Rate limiting returns 429
 * - Error response format includes error_code
 * - No PII leaked in responses (emails logged, but not in response body beyond what's needed)
 */
@WebMvcTest(AuthController.class)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private RateLimitingService rateLimitingService;

    @MockBean
    private RsaKeyManager rsaKeyManager;

    @MockBean
    private AuthCookieService authCookieService;

    // Spring Security dependencies
    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Default: allow all rate limit requests
        when(rateLimitingService.isAllowed(anyString(), anyInt(), anyLong())).thenReturn(true);
        when(authCookieService.createAccessTokenCookie(anyString()))
            .thenReturn(ResponseCookie.from("lh_access_token", "value").httpOnly(true).path("/").build());
        when(authCookieService.createRefreshTokenCookie(anyString()))
            .thenReturn(ResponseCookie.from("lh_refresh_token", "value").httpOnly(true).path("/api/v1/auth").build());
        when(authCookieService.clearAccessTokenCookie())
            .thenReturn(ResponseCookie.from("lh_access_token", "").httpOnly(true).path("/").maxAge(0).build());
        when(authCookieService.clearRefreshTokenCookie())
            .thenReturn(ResponseCookie.from("lh_refresh_token", "").httpOnly(true).path("/api/v1/auth").maxAge(0).build());
    }

    // =========================================================================
    // ENDPOINT 1: POST /api/v1/auth/register
    // =========================================================================

    @Test
    @DisplayName("POST /register — 201 on success")
    void register_success_returns201() throws Exception {
        RegisterRequest req = new RegisterRequest("test@example.com", "Str0ng@Pass1234", "Alice", "Smith");
        com.learnhub.auth.dto.RegisterResponse resp =
            new com.learnhub.auth.dto.RegisterResponse(UUID.randomUUID(), "test@example.com", "STUDENT");

        when(authService.register(anyString(), anyString(), anyString(), anyString())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    @DisplayName("POST /register — 409 when email already registered")
    void register_duplicateEmail_returns409() throws Exception {
        RegisterRequest req = new RegisterRequest("dup@example.com", "Str0ng@Pass1234", "Bob", "Jones");
        when(authService.register(anyString(), anyString(), anyString(), anyString()))
            .thenThrow(new AuthService.EmailAlreadyRegisteredException("Email is already registered"));

        mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error_code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    @DisplayName("POST /register — 400 when password is weak")
    void register_weakPassword_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest("x@example.com", "Str0ng@Pass1234", "X", "Y");
        when(authService.register(anyString(), anyString(), anyString(), anyString()))
            .thenThrow(new AuthService.WeakPasswordException("Password must contain at least one special character"));

        mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error_code").value("WEAK_PASSWORD"));
    }

    @Test
    @DisplayName("POST /register — 400 when email format invalid")
    void register_invalidEmail_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest("not-an-email", "Str0ng@Pass1234", "X", "Y");

        mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // ENDPOINT 2: POST /api/v1/auth/verify-email
    // =========================================================================

    @Test
    @DisplayName("POST /verify-email — 200 on success")
    void verifyEmail_success_returns200() throws Exception {
        doNothing().when(authService).verifyEmail(anyString());

        mockMvc.perform(post("/api/v1/auth/verify-email")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new VerifyEmailRequest("valid-token"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Email verified"))
            .andExpect(jsonPath("$.redirectUrl").value("/login"));
    }

    @Test
    @DisplayName("POST /verify-email — 400 on invalid token")
    void verifyEmail_invalidToken_returns400() throws Exception {
        doThrow(new com.learnhub.user.exception.InvalidTokenException("Expired"))
            .when(authService).verifyEmail(anyString());

        mockMvc.perform(post("/api/v1/auth/verify-email")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new VerifyEmailRequest("bad-token"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error_code").value("INVALID_TOKEN"));
    }

    // =========================================================================
    // ENDPOINT 3: POST /api/v1/auth/login
    // =========================================================================

    @Test
    @DisplayName("POST /login — 200 on success")
    void login_success_returns200() throws Exception {
        com.learnhub.auth.dto.AuthResponse resp = new com.learnhub.auth.dto.AuthResponse(
            UUID.randomUUID(), "STUDENT", "u@t.com", "access-jwt", "refresh-jwt", 900L);
        when(authService.login(anyString(), anyString())).thenReturn(resp);

        LoginRequest req = new LoginRequest("u@t.com", "pass");
        mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(header().exists("Set-Cookie"))
            .andExpect(jsonPath("$.token").doesNotExist())
            .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    @DisplayName("POST /login — 401 on invalid credentials")
    void login_invalidCredentials_returns401() throws Exception {
        when(authService.login(anyString(), anyString()))
            .thenThrow(new AuthService.InvalidCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new LoginRequest("u@t.com", "wrong"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error_code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("POST /login — 429 when rate limit exceeded")
    void login_rateLimitExceeded_returns429() throws Exception {
        when(rateLimitingService.isAllowed(anyString(), eq(5), eq(60L))).thenReturn(false);
        when(rateLimitingService.secondsUntilReset(anyString(), eq(60L))).thenReturn(45L);

        mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new LoginRequest("u@t.com", "p"))))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"))
            .andExpect(jsonPath("$.error_code").value("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    @DisplayName("POST /login — 423 when account locked")
    void login_accountLocked_returns423() throws Exception {
        when(authService.login(anyString(), anyString()))
            .thenThrow(new com.learnhub.user.exception.AccountLockedException("Locked", 25L));

        mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new LoginRequest("u@t.com", "pass"))))
            .andExpect(status().is(423));
    }

    // =========================================================================
    // ENDPOINT 4: POST /api/v1/auth/logout
    // =========================================================================

    @Test
    @DisplayName("POST /logout — 200 always (idempotent)")
    void logout_alwaysReturns200() throws Exception {
        doNothing().when(authService).logout(any());
        when(authCookieService.extractRefreshToken(any())).thenReturn("cookie-refresh-token");

        mockMvc.perform(post("/api/v1/auth/logout")
            .header("Authorization", "Bearer some-refresh-token"))
            .andExpect(status().isOk())
            .andExpect(header().exists("Set-Cookie"))
            .andExpect(jsonPath("$.message").value("Logged out"));
    }

    // =========================================================================
    // ENDPOINT 5: POST /api/v1/auth/forgot-password
    // =========================================================================

    @Test
    @DisplayName("POST /forgot-password — always returns 200 (prevents enumeration)")
    void forgotPassword_alwaysReturns200() throws Exception {
        doNothing().when(authService).forgotPassword(anyString());

        mockMvc.perform(post("/api/v1/auth/forgot-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"any@example.com\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").exists());
    }

    // =========================================================================
    // ENDPOINT 6: POST /api/v1/auth/reset-password
    // =========================================================================

    @Test
    @DisplayName("POST /reset-password — 200 on success")
    void resetPassword_success_returns200() throws Exception {
        doNothing().when(authService).resetPassword(anyString(), anyString());

        mockMvc.perform(post("/api/v1/auth/reset-password")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\":\"valid\",\"newPassword\":\"NewStr0ng@Pass!\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").exists());
    }

    // =========================================================================
    // ENDPOINT 7: POST /api/v1/auth/refresh
    // =========================================================================

    @Test
    @DisplayName("POST /refresh — 200 with new tokens")
    void refresh_success_returns200() throws Exception {
        com.learnhub.auth.dto.TokenRefreshResponse resp =
            new com.learnhub.auth.dto.TokenRefreshResponse("new-access", "new-refresh", 900L);
        when(authService.refresh(anyString())).thenReturn(resp);
        when(authCookieService.extractRefreshToken(any())).thenReturn("cookie-refresh-token");

        mockMvc.perform(post("/api/v1/auth/refresh")
            .header("Authorization", "Bearer old-refresh-token"))
            .andExpect(status().isOk())
            .andExpect(header().exists("Set-Cookie"))
            .andExpect(jsonPath("$.token").doesNotExist())
            .andExpect(jsonPath("$.refreshToken").doesNotExist())
            .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    @DisplayName("POST /refresh — 401 without refresh cookie or fallback header")
    void refresh_noHeader_returns401() throws Exception {
        when(authCookieService.extractRefreshToken(any())).thenReturn(null);
        mockMvc.perform(post("/api/v1/auth/refresh"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error_code").value("MISSING_TOKEN"));
    }

    // =========================================================================
    // ENDPOINT 11: GET /api/v1/auth/public-key/{version}
    // =========================================================================

    @Test
    @DisplayName("GET /public-key/v1 — 200 with PEM public key")
    void getPublicKey_v1_returns200() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        RSAPublicKey pubKey = (RSAPublicKey) gen.generateKeyPair().getPublic();
        when(rsaKeyManager.getPublicKeyForVersion(1)).thenReturn(pubKey);
        when(rsaKeyManager.getKeyVersion()).thenReturn(1);

        mockMvc.perform(get("/api/v1/auth/public-key/v1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.publicKey").value(org.hamcrest.Matchers.containsString("BEGIN PUBLIC KEY")))
            .andExpect(jsonPath("$.version").value("v1"));
    }

    @Test
    @DisplayName("GET /public-key/v999 — 404 if version not found")
    void getPublicKey_unknownVersion_returns404() throws Exception {
        when(rsaKeyManager.getPublicKeyForVersion(999)).thenReturn(null);

        mockMvc.perform(get("/api/v1/auth/public-key/v999"))
            .andExpect(status().isNotFound());
    }
}
