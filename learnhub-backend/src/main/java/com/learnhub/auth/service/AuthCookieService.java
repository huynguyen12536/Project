package com.learnhub.auth.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieService {

    public static final String ACCESS_TOKEN_COOKIE = "lh_access_token";
    public static final String REFRESH_TOKEN_COOKIE = "lh_refresh_token";

    private final long accessTokenTtlSeconds;
    private final long refreshTokenTtlSeconds;
    private final boolean secureCookies;
    private final String sameSite;

    public AuthCookieService(
        @Value("${jwt.access-token-ttl-seconds:900}") long accessTokenTtlSeconds,
        @Value("${jwt.refresh-token-ttl-seconds:2592000}") long refreshTokenTtlSeconds,
        @Value("${app.security.cookies.secure:false}") boolean secureCookies,
        @Value("${app.security.cookies.same-site:Lax}") String sameSite
    ) {
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
        this.secureCookies = secureCookies;
        this.sameSite = sameSite;
    }

    public ResponseCookie createAccessTokenCookie(String token) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, token)
            .httpOnly(true)
            .secure(secureCookies)
            .sameSite(sameSite)
            .path("/")
            .maxAge(accessTokenTtlSeconds)
            .build();
    }

    public ResponseCookie createRefreshTokenCookie(String token) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, token)
            .httpOnly(true)
            .secure(secureCookies)
            .sameSite(sameSite)
            .path("/api/v1/auth")
            .maxAge(refreshTokenTtlSeconds)
            .build();
    }

    public ResponseCookie clearAccessTokenCookie() {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, "")
            .httpOnly(true)
            .secure(secureCookies)
            .sameSite(sameSite)
            .path("/")
            .maxAge(0)
            .build();
    }

    public ResponseCookie clearRefreshTokenCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
            .httpOnly(true)
            .secure(secureCookies)
            .sameSite(sameSite)
            .path("/api/v1/auth")
            .maxAge(0)
            .build();
    }

    public String extractAccessToken(HttpServletRequest request) {
        return extractCookieValue(request, ACCESS_TOKEN_COOKIE);
    }

    public String extractRefreshToken(HttpServletRequest request) {
        return extractCookieValue(request, REFRESH_TOKEN_COOKIE);
    }

    private String extractCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
