package com.learnhub.assessment.test;

import redis.embedded.RedisServer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages embedded Redis instance for testing.
 *
 * Singleton pattern ensures only one Redis instance runs during test suite.
 * Automatically starts on first use and stops when JVM exits.
 *
 * This is simpler than Testcontainers for Redis and avoids Docker dependency.
 * For production environments or complex Redis setups, consider using
 * testcontainers/redis instead.
 */
@Slf4j
public class EmbeddedRedisConfig {

    private static final EmbeddedRedisConfig INSTANCE = new EmbeddedRedisConfig();
    private static final int REDIS_PORT = 6380;  // Use non-standard port to avoid conflicts
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);

    private RedisServer redisServer;

    private EmbeddedRedisConfig() {
        try {
            this.redisServer = new RedisServer(REDIS_PORT);
            this.redisServer.start();
            STARTED.set(true);
            log.info("Embedded Redis started on port {}", REDIS_PORT);

            // Graceful shutdown on JVM exit
            Runtime.getRuntime().addShutdownHook(new Thread(this::stopRedis));
        } catch (IOException e) {
            log.error("Failed to start embedded Redis", e);
            throw new RuntimeException("Cannot start embedded Redis", e);
        }
    }

    /**
     * Get singleton instance (creates if not exists).
     * @return EmbeddedRedisConfig instance
     */
    public static EmbeddedRedisConfig getInstance() {
        return INSTANCE;
    }

    /**
     * Get Redis port.
     * @return port number
     */
    public int getPort() {
        return REDIS_PORT;
    }

    /**
     * Check if Redis is running.
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return STARTED.get();
    }

    /**
     * Stop Redis server (called automatically on JVM exit).
     */
    private void stopRedis() {
        if (redisServer != null && STARTED.get()) {
            try {
                redisServer.stop();
                STARTED.set(false);
                log.info("Embedded Redis stopped");
            } catch (IOException e) {
                log.warn("Error stopping embedded Redis", e);
            }
        }
    }
}
