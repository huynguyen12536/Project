package com.learnhub.config;

import com.learnhub.auth.filter.JwtAuthenticationFilter;
import com.learnhub.auth.service.JwtService;
import com.learnhub.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(
        JwtService jwtService,
        UserRepository userRepository,
        @Qualifier("corsConfigurationSource")
        CorsConfigurationSource corsConfigurationSource
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Enable CORS from the centralized configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public auth endpoints (register, login, verify-email, forgot/reset password, token refresh)
                        .requestMatchers(
                            "/api/v1/auth/register",
                            "/api/v1/auth/login",
                            "/api/v1/auth/verify-email",
                            "/api/v1/auth/forgot-password",
                            "/api/v1/auth/reset-password",
                            "/api/v1/auth/refresh",
                            "/api/v1/auth/logout"
                        ).permitAll()
                        // Public key endpoint — no auth required (used by clients to verify JWTs)
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/public-key/**").permitAll()
                        // Admin endpoints — secured by @PreAuthorize on controller methods
                        .requestMatchers("/api/v1/auth/revoke-sessions", "/api/v1/auth/key-rotation").authenticated()
                        // GitHub OAuth callbacks are public (no JWT yet at callback time)
                        .requestMatchers(HttpMethod.GET, "/api/v1/oauth/github/callback").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/github/callback").permitAll()
                        // Legacy email/password endpoints (kept for backward compatibility)
                        .requestMatchers("/api/v1/email/verify").permitAll()
                        .requestMatchers("/api/v1/password/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        // CORS preflight OPTIONS requests are handled before authorization checks
                        .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                        // All other requests require authentication
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());

        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtService, userRepository);
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Cost factor 13 meets OWASP minimum recommended strength (2^13 iterations)
        return new BCryptPasswordEncoder(13);
    }
}
