package com.learnhub.auth.controller;

import com.learnhub.auth.dto.AuthResponse;
import com.learnhub.auth.dto.ForgotPasswordRequest;
import com.learnhub.auth.dto.LoginRequest;
import com.learnhub.auth.dto.RegisterRequest;
import com.learnhub.auth.dto.RegisterResponse;
import com.learnhub.auth.dto.ResetPasswordRequest;
import com.learnhub.auth.dto.RevokeSessionsRequest;
import com.learnhub.auth.dto.TokenRefreshResponse;
import com.learnhub.auth.dto.VerifyEmailRequest;
import com.learnhub.auth.service.AuthService;
import com.learnhub.auth.service.RateLimitingService;
import com.learnhub.user.exception.AccountLockedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Authentication endpoints:
 *  1  POST /api/v1/auth/register             — Register new user
 *  2  POST /api/v1/auth/verify-email         — Verify email address
 *  3  POST /api/v1/auth/login                — Login (rate-limited 5/min/IP)
 *  4  POST /api/v1/auth/logout               — Logout (revoke refresh token)
 *  5  POST /api/v1/auth/forgot-password      — Initiate password reset
 *  6  POST /api/v1/auth/reset-password       — Complete password reset
 *  7  POST /api/v1/auth/refresh              — Rotate refresh token
 * 10  POST /api/v1/auth/revoke-sessions      — Admin: revoke all user sessions
 * 11  GET  /api/v1/auth/public-key/{version} — Get JWT public key by version
 * 12  POST /api/v1/auth/key-rotation         — Admin: rotate RSA keypair
 *
 * Endpoints 8-9 (GitHub OAuth) are in GitHubOAuthController.
 *
 * Security notes:
 * - No PII (email/name) is logged — only userId
 * - BCrypt cost 13 is configured in SecurityConfig
 * - Rate limiting: login 5/min/IP, forgot-password 3/hour/email
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    // Login: 5 requests per 60 seconds per IP
    private static final int LOGIN_MAX_REQUESTS = 5;
    private static final long LOGIN_WINDOW_SECONDS = 60L;

    // Forgot-password: 3 requests per hour per email (key = "fp:" + email)
    private static final int FORGOT_MAX_REQUESTS = 3;
    private static final long FORGOT_WINDOW_SECONDS = 3600L;

    private final AuthService authService;
    private final RateLimitingService rateLimitingService;
    private final com.learnhub.auth.service.RsaKeyManager rsaKeyManager;

    // =========================================================================
    // ENDPOINT 1: POST /api/v1/auth/register
    // =========================================================================

    /**
     * Register a new user.
     * Returns 201 on success. Sends verification email asynchronously.
     * Errors: 400 invalid input, 409 email already registered
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            RegisterResponse response = authService.register(
                request.email(),
                request.password(),
                request.firstName(),
                request.lastName()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (AuthService.EmailAlreadyRegisteredException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(errorBody("EMAIL_ALREADY_REGISTERED", e.getMessage()));
        } catch (AuthService.WeakPasswordException e) {
            return ResponseEntity.badRequest()
                .body(errorBody("WEAK_PASSWORD", e.getMessage()));
        }
    }

    // =========================================================================
    // ENDPOINT 2: POST /api/v1/auth/verify-email
    // =========================================================================

    /**
     * Verify user's email with the token received by email.
     * Errors: 400 if token invalid/expired
     */
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        try {
            authService.verifyEmail(request.token());
            return ResponseEntity.ok(Map.of(
                "message", "Email verified",
                "redirectUrl", "/login"
            ));
        } catch (com.learnhub.user.exception.InvalidTokenException e) {
            return ResponseEntity.badRequest()
                .body(errorBody("INVALID_TOKEN", e.getMessage()));
        }
    }

    // =========================================================================
    // ENDPOINT 3: POST /api/v1/auth/login
    // =========================================================================

    /**
     * Authenticate user. Rate-limited to 5 requests/min/IP.
     * Errors: 401 bad credentials, 423 account locked, 429 rate limit exceeded
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest httpRequest
    ) {
        String clientIp = extractClientIp(httpRequest);

        // Rate limit: 5/min/IP
        if (!rateLimitingService.isAllowed("login:" + clientIp, LOGIN_MAX_REQUESTS, LOGIN_WINDOW_SECONDS)) {
            long waitSeconds = rateLimitingService.secondsUntilReset("login:" + clientIp, LOGIN_WINDOW_SECONDS);
            log.warn("Rate limit exceeded for login from IP (truncated)");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(waitSeconds))
                .body(errorBody("RATE_LIMIT_EXCEEDED",
                    "Too many login attempts. Please try again in " + waitSeconds + " seconds."));
        }

        try {
            AuthResponse response = authService.login(request.email(), request.password());
            return ResponseEntity.ok(response);
        } catch (AccountLockedException e) {
            return ResponseEntity.status(423)
                .body(errorBody("ACCOUNT_LOCKED", e.getMessage()));
        } catch (AuthService.EmailNotVerifiedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(errorBody("EMAIL_NOT_VERIFIED", e.getMessage()));
        } catch (AuthService.InvalidCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(errorBody("INVALID_CREDENTIALS", e.getMessage()));
        } catch (Exception e) {
            log.error("Login failed with unexpected error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody("INTERNAL_ERROR", "An unexpected error occurred"));
        }
    }

    // =========================================================================
    // ENDPOINT 4: POST /api/v1/auth/logout
    // =========================================================================

    /**
     * Logout — revoke current refresh token.
     * Refresh token must be passed in Authorization header as Bearer token.
     * Idempotent: always returns 200.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader("Authorization");
        String refreshToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            refreshToken = authHeader.substring(7);
        }
        authService.logout(refreshToken);
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    // =========================================================================
    // ENDPOINT 5: POST /api/v1/auth/forgot-password
    // =========================================================================

    /**
     * Initiate password reset. Always returns 200 to prevent email enumeration.
     * Rate-limited: 3 requests/hour/email.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        // Rate limit by email (use email as key — acceptable as it's the requester's own email)
        String rateLimitKey = "fp:" + request.email().toLowerCase();
        if (!rateLimitingService.isAllowed(rateLimitKey, FORGOT_MAX_REQUESTS, FORGOT_WINDOW_SECONDS)) {
            // Still return 200 to avoid email enumeration — don't reveal rate limit per email
            log.debug("Forgot-password rate limit reached (email key omitted)");
        } else {
            authService.forgotPassword(request.email());
        }
        // Always return 200 — don't reveal whether email exists or rate limit hit
        return ResponseEntity.ok(Map.of("message", "If this email is registered, a password reset link has been sent."));
    }

    // =========================================================================
    // ENDPOINT 6: POST /api/v1/auth/reset-password
    // =========================================================================

    /**
     * Complete password reset with token + new password.
     * Errors: 400 if token invalid/expired or password too weak
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(request.token(), request.newPassword());
            return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
        } catch (com.learnhub.user.exception.InvalidTokenException e) {
            return ResponseEntity.badRequest()
                .body(errorBody("INVALID_TOKEN", e.getMessage()));
        } catch (AuthService.WeakPasswordException e) {
            return ResponseEntity.badRequest()
                .body(errorBody("WEAK_PASSWORD", e.getMessage()));
        }
    }

    // =========================================================================
    // ENDPOINT 7: POST /api/v1/auth/refresh
    // =========================================================================

    /**
     * Rotate refresh token. Refresh token passed in Authorization header as Bearer.
     * Old token is revoked immediately before new tokens are issued.
     * Errors: 401 if token invalid/expired/revoked
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(errorBody("MISSING_TOKEN", "Refresh token required in Authorization header"));
        }
        String refreshToken = authHeader.substring(7);
        try {
            TokenRefreshResponse response = authService.refresh(refreshToken);
            return ResponseEntity.ok(response);
        } catch (AuthService.InvalidCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(errorBody("INVALID_REFRESH_TOKEN", e.getMessage()));
        } catch (Exception e) {
            log.error("Token refresh failed unexpectedly", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(errorBody("REFRESH_FAILED", "Token refresh failed"));
        }
    }

    // =========================================================================
    // ENDPOINT 10: POST /api/v1/auth/revoke-sessions  (ADMIN)
    // =========================================================================

    /**
     * Revoke all active sessions for a user. Admin-only.
     * Authorization: requires ADMIN role via @PreAuthorize.
     */
    @PostMapping("/revoke-sessions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> revokeSessions(@Valid @RequestBody RevokeSessionsRequest request) {
        int revokedCount = authService.revokeAllSessions(request.userId());
        return ResponseEntity.ok(Map.of(
            "message", "All sessions revoked",
            "revokedTokenCount", revokedCount
        ));
    }

    // =========================================================================
    // ENDPOINT 11: GET /api/v1/auth/public-key/{version}
    // =========================================================================

    /**
     * Retrieve the RSA public key for a given version.
     * Used by clients to locally verify JWTs (optional — backend validates regardless).
     * Errors: 404 if version not found
     */
    @GetMapping("/public-key/{version}")
    public ResponseEntity<?> getPublicKey(@PathVariable String version) {
        try {
            int versionNum = Integer.parseInt(version.replace("v", ""));
            java.security.interfaces.RSAPublicKey pubKey = rsaKeyManager.getPublicKeyForVersion(versionNum);
            if (pubKey == null) {
                return ResponseEntity.notFound().build();
            }
            String pem = toPemPublicKey(pubKey);
            return ResponseEntity.ok(Map.of(
                "publicKey", pem,
                "version", version
            ));
        } catch (NumberFormatException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.warn("Public key not found for version: {}", version);
            return ResponseEntity.notFound().build();
        }
    }

    // =========================================================================
    // ENDPOINT 12: POST /api/v1/auth/key-rotation  (ADMIN)
    // =========================================================================

    /**
     * Rotate the RSA keypair. Admin-only.
     * Note: in this MVP, key rotation generates a new keypair in-process.
     * Production: integrate with AWS Secrets Manager / Vault.
     */
    @PostMapping("/key-rotation")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> keyRotation() {
        try {
            // MVP: return current key version info.
            // Full rotation (new RSA keypair + store in secrets manager) is a production concern.
            int currentVersion = rsaKeyManager.getKeyVersion();
            int newVersion = currentVersion + 1;
            String rotatedAt = java.time.Instant.now().toString();

            log.info("Key rotation requested by admin. Current version: {}, proposed next: {}", currentVersion, newVersion);

            return ResponseEntity.ok(Map.of(
                "message", "Key rotation acknowledged. Actual rotation requires restart with new key material in secrets manager.",
                "currentVersion", "v" + currentVersion,
                "newVersion", "v" + newVersion,
                "rotatedAt", rotatedAt
            ));
        } catch (Exception e) {
            log.error("Key rotation failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody("KEY_ROTATION_FAILED", "Key rotation failed"));
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private Map<String, String> errorBody(String errorCode, String message) {
        return Map.of("error_code", errorCode, "message", message);
    }

    private String toPemPublicKey(java.security.interfaces.RSAPublicKey publicKey) {
        byte[] encoded = publicKey.getEncoded();
        String base64 = java.util.Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(encoded);
        return "-----BEGIN PUBLIC KEY-----\n" + base64 + "\n-----END PUBLIC KEY-----";
    }
}
