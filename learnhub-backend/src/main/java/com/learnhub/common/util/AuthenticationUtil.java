package com.learnhub.common.util;

import com.learnhub.user.model.User;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class AuthenticationUtil {

    /**
     * Extracts the current user's UUID from the SecurityContext.
     * Handles multiple principal types:
     * 1. User object stored in authentication details (preferred - set by JwtAuthenticationFilter)
     * 2. SignedJWT principal (if JWT is set directly)
     * 3. String principal with UUID subject (fallback)
     */
    public UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }

        // Approach 1: Extract User from authentication details (JwtAuthenticationFilter stores it here)
        if (authentication.getDetails() instanceof User userDetails) {
            UUID userId = userDetails.getId();
            if (userId != null) {
                log.debug("Extracted user ID from authentication details: {}", userId);
                return userId;
            }
        }

        // Approach 2: Handle SignedJWT principal (direct JWT token)
        if (authentication.getPrincipal() instanceof SignedJWT signedJWT) {
            try {
                JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
                String subClaim = claims.getSubject();
                if (subClaim != null) {
                    UUID userId = UUID.fromString(subClaim);
                    log.debug("Extracted user ID from SignedJWT principal: {}", userId);
                    return userId;
                }
            } catch (Exception e) {
                log.warn("Error extracting user ID from SignedJWT principal", e);
            }
        }

        // Approach 3: Fallback - if principal is String (email), we cannot extract UUID
        // This would require a database lookup, which is not appropriate in this utility.
        // The filter should ensure the User object is in details or JWT is properly parsed.
        String principal = authentication.getPrincipal().toString();
        log.error("Cannot extract user ID - authentication principal is string: {}. " +
                  "Expected User object in details or SignedJWT principal.", principal);

        throw new IllegalStateException(
            "Unable to extract user ID from authentication. " +
            "Principal type: " + authentication.getPrincipal().getClass().getSimpleName()
        );
    }

    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }

        // Handle Nimbus JWT
        if (authentication.getPrincipal() instanceof SignedJWT signedJWT) {
            try {
                JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
                Object emailClaim = claims.getClaim("email");
                if (emailClaim != null) {
                    return emailClaim.toString();
                }
            } catch (Exception e) {
                log.error("Error extracting email from JWT", e);
            }
        }

        return authentication.getName();
    }

    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }
}
