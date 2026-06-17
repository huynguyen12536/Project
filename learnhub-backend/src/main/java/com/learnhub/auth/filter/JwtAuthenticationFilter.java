package com.learnhub.auth.filter;

import com.learnhub.auth.service.JwtService;
import com.learnhub.user.model.User;
import com.learnhub.user.repository.UserRepository;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtService.validateToken(token)) {
                try {
                    SignedJWT jwt = SignedJWT.parse(token);
                    String userId = jwt.getJWTClaimsSet().getSubject();
                    String roles = (String) jwt.getJWTClaimsSet().getClaim("roles");

                    Optional<User> userOptional = userRepository.findById(java.util.UUID.fromString(userId));
                    if (userOptional.isPresent()) {
                        User user = userOptional.get();
                        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + (roles != null ? roles : "LEARNER"));
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(user.getEmail(), null, Collections.singletonList(authority));
                        authToken.setDetails(user);
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                } catch (Exception ex) {
                    logger.error("Failed to parse JWT token", ex);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
