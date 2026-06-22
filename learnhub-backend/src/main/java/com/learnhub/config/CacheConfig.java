package com.learnhub.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration for the application.
 *
 * Enables Spring's @Cacheable annotation support.
 * Cache provider is configured via application.yml:
 *
 * spring:
 *   cache:
 *     type: caffeine
 *     caffeine:
 *       spec: maximumSize=100,expireAfterWrite=24h
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
