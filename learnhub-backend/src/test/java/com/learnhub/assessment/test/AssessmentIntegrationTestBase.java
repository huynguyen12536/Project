package com.learnhub.assessment.test;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests using Testcontainers.
 *
 * Provides:
 * - PostgreSQL container for database testing
 * - Redis container for queue and caching tests
 * - Automatic container lifecycle management
 * - Dynamic property registry for Spring configuration
 *
 * Extend this class in all integration tests that require database
 * and Redis infrastructure.
 *
 * Usage:
 * {@code
 * @SpringBootTest
 * class MyIntegrationTest extends AssessmentIntegrationTestBase {
 *     @Test
 *     void testSomething() {
 *         // Container is already started and configured
 *     }
 * }
 * }
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Slf4j
public abstract class AssessmentIntegrationTestBase {

    /**
     * PostgreSQL container shared across all tests.
     * Automatically started and stopped by Testcontainers.
     */
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("learnhub_test")
            .withUsername("test_user")
            .withPassword("test_password")
            .withInitScript("db/init-test.sql")
            .withLogConsumer(new Slf4jLogConsumer(log));

    /**
     * Embedded Redis instance for testing.
     * Note: Using embedded Redis instead of Testcontainers container
     * for simplicity. For production test environments, consider
     * using testcontainers/redis container.
     */
    protected static EmbeddedRedisConfig redisConfig = EmbeddedRedisConfig.getInstance();

    /**
     * Dynamically register PostgreSQL properties with Spring.
     * Called by Spring test framework before test context creation.
     *
     * @param registry dynamic property registry
     */
    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL configuration
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Redis configuration (using embedded Redis)
        registry.add("spring.redis.host", () -> "localhost");
        registry.add("spring.redis.port", () -> redisConfig.getPort());

        // Flyway configuration for testing
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");

        log.info("Test infrastructure configured:");
        log.info("  - PostgreSQL: {} @ {}", postgres.getDatabaseName(), postgres.getJdbcUrl());
        log.info("  - Redis: localhost:{}", redisConfig.getPort());
    }
}
