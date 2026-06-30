package com.learnhub.auth.filter;

import com.learnhub.auth.service.JwtService;
import com.learnhub.auth.service.AuthCookieService;
import com.learnhub.user.model.User;
import com.learnhub.user.repository.UserRepository;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AuthCookieService authCookieService;

    public JwtAuthenticationFilter(
        JwtService jwtService,
        UserRepository userRepository,
        AuthCookieService authCookieService
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.authCookieService = authCookieService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        logger.info("JWT_FILTER_INVOKED: {} {}", request.getMethod(), request.getRequestURI());
        String authHeader = request.getHeader("Authorization");
        String token = authCookieService.extractAccessToken(request);
        logger.info("JWT_FILTER_AUTH_HEADER_PRESENT: {}", authHeader != null);
        logger.info("JWT_FILTER_ACCESS_COOKIE_PRESENT: {}", token != null);

        if (token == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        if (token != null && !token.isBlank()) {
            if (jwtService.validateToken(token)) {
                try {
                    SignedJWT jwt = SignedJWT.parse(token);
                    String userId = jwt.getJWTClaimsSet().getSubject();
                    String roles = (String) jwt.getJWTClaimsSet().getClaim("roles");

                    Optional<User> userOptional = userRepository.findById(java.util.UUID.fromString(userId));
                    if (userOptional.isPresent()) {
                        User user = userOptional.get();
                        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + (roles != null ? roles : "LEARNER"));
                        // Use userId (UUID) as principal — never store PII (email) in SecurityContext
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(userId, null, Collections.singletonList(authority));
                        authToken.setDetails(user);
                        SecurityContextHolder.getContext().setAuthentication(authToken);

                        // DEBUG LOGGING - Verify authentication chain (userId only, never PII)
                        logger.info("JWT_FILTER: Token parsed successfully for userId: {}", userId);
                        logger.info("JWT_CLAIM_ROLES: {}", roles);
                        logger.info("AUTH_TYPE: {}", authToken.getClass().getSimpleName());
                        logger.info("AUTHORITIES: {}", authToken.getAuthorities());
                        logger.info("AUTH_AUTHENTICATED: {}", authToken.isAuthenticated());
                        logger.info("SECURITYCONTEXT_AUTHENTICATION: {}", SecurityContextHolder.getContext().getAuthentication().getClass().getSimpleName());
                        logger.info("SECURITYCONTEXT_AUTHORITIES: {}", SecurityContextHolder.getContext().getAuthentication().getAuthorities());
                    }
                } catch (Exception ex) {
                    logger.error("Failed to parse JWT token", ex);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
