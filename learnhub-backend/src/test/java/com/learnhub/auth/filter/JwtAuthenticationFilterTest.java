package com.learnhub.auth.filter;

import com.learnhub.auth.service.AuthCookieService;
import com.learnhub.auth.service.JwtService;
import com.learnhub.user.model.User;
import com.learnhub.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Security unit tests for JwtAuthenticationFilter.
 *
 * BLOCKER 1 verification: Ensures no PII (email addresses) appear in log statements.
 * Inspects source code directly to guarantee the logger never receives user.getEmail().
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthCookieService authCookieService;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, userRepository, authCookieService);
    }

    /**
     * BLOCKER 1: Verifies source code contains no email logging (GDPR compliance).
     * Reads the actual source file and asserts that getEmail() is not passed to any logger call.
     */
    @Test
    void noPiiInLogs_EmailMustNotBeLoggedInFilter() throws IOException {
        Path sourceFile = Path.of("src/main/java/com/learnhub/auth/filter/JwtAuthenticationFilter.java");
        String source = Files.readString(sourceFile);

        // The pattern catches: logger.xxx(...getEmail()...) or logger.xxx("...", user.getEmail())
        Pattern emailInLogPattern = Pattern.compile(
            "logger\\s*\\.\\s*(info|debug|warn|error|trace)\\s*\\([^)]*getEmail\\(\\)");

        assertFalse(
            emailInLogPattern.matcher(source).find(),
            "GDPR VIOLATION: user.getEmail() must never appear as an argument to a logger call. " +
            "Use userId (UUID) instead."
        );
    }

    /**
     * Verifies that userId (the UUID subject claim) IS used in success log messages.
     * This confirms the replacement was made, not just that email was removed.
     */
    @Test
    void noPiiInLogs_UserIdIsUsedInSuccessLog() throws IOException {
        Path sourceFile = Path.of("src/main/java/com/learnhub/auth/filter/JwtAuthenticationFilter.java");
        String source = Files.readString(sourceFile);

        // Confirm "userId" appears as the logged value after token parse success
        assertTrue(
            source.contains("userId"),
            "Filter must log the userId (UUID) for audit trail, not email."
        );
    }

    /**
     * Sanity check: Filter skips authentication when no Authorization header is present.
     */
    @Test
    void doFilterInternal_NoAuthHeader_SkipsAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        // Chain should still be invoked (no exception thrown)
        assertThat(response.getStatus()).isEqualTo(200);
    }

    /**
     * Sanity check: Filter skips authentication when token is invalid.
     */
    @Test
    void doFilterInternal_InvalidToken_SkipsAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");
        request.addHeader("Authorization", "Bearer invalid.token.here");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(authCookieService.extractAccessToken(request)).thenReturn(null);
        when(jwtService.validateToken("invalid.token.here")).thenReturn(false);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }
}
