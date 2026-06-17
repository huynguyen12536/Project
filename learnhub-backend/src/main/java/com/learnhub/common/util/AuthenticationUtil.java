package com.learnhub.common.util;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class AuthenticationUtil {

    public UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }

        // Handle Spring Security JWT
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            String subClaim = jwt.getClaimAsString("sub");
            if (subClaim != null) {
                return UUID.fromString(subClaim);
            }
        }

        // Handle Nimbus JWT (for custom JWT handling if needed)
        if (authentication.getPrincipal() instanceof SignedJWT signedJWT) {
            try {
                JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
                String subClaim = claims.getSubject();
                if (subClaim != null) {
                    return UUID.fromString(subClaim);
                }
            } catch (Exception e) {
                log.error("Error extracting user ID from JWT", e);
            }
        }

        throw new IllegalStateException("Unable to extract user ID from authentication");
    }

    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }

        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaimAsString("email");
        }

        return authentication.getName();
    }

    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }
}
