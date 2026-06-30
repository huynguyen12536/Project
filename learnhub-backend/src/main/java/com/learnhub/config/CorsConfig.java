package com.learnhub.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS Configuration for LearnHub Backend.
 * Enables cross-origin requests from frontend applications for real-time SSE (Server-Sent Events) connections
 * and standard API calls.
 *
 * Configuration:
 * - Allows Origins: http://localhost:3000 (React dev), http://localhost:5173 (Vite dev), and production domain
 * - Allowed Methods: GET, POST, PUT, DELETE, OPTIONS, PATCH
 * - Allowed Headers: Content-Type, Authorization, Accept
 * - Exposed Headers: X-Total-Count, X-Page-Number (for pagination)
 * - Allow Credentials: true (for cookies/JWT in requests)
 * - Max Age: 3600 seconds (1 hour preflight caching)
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Parse allowed origins from configuration
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isBlank())
            .toList();
        configuration.setAllowedOrigins(origins);

        // Allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Allowed request headers
        configuration.setAllowedHeaders(Arrays.asList(
            "Content-Type",
            "Authorization",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));

        // Exposed response headers
        configuration.setExposedHeaders(Arrays.asList(
            "Content-Type",
            "X-Total-Count",
            "X-Page-Number",
            "X-Total-Pages",
            "Access-Control-Allow-Origin"
        ));

        // Allow credentials (JWT tokens, cookies)
        configuration.setAllowCredentials(true);

        // Max age for preflight cache (1 hour)
        configuration.setMaxAge(3600L);

        // Apply to all API endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }
}
