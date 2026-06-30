package com.learnhub.auth.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory token bucket rate limiter.
 *
 * Tracks request counts per key (IP or email) within a configurable time window.
 * Thread-safe via ConcurrentHashMap and AtomicInteger.
 *
 * No PII is stored or logged — callers should pass anonymized keys (e.g., hashed email or IP).
 */
@Service
public class RateLimitingService {

    private static final class Bucket {
        final AtomicInteger count = new AtomicInteger(0);
        volatile long windowStartEpoch;

        Bucket(long windowStartEpoch) {
            this.windowStartEpoch = windowStartEpoch;
        }
    }

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Check whether the given key is within the allowed rate.
     *
     * @param key         rate limit key (e.g., IP address or hashed email)
     * @param maxRequests maximum requests allowed in the window
     * @param windowSeconds window duration in seconds
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean isAllowed(String key, int maxRequests, long windowSeconds) {
        long now = Instant.now().getEpochSecond();

        Bucket bucket = buckets.compute(key, (k, existing) -> {
            if (existing == null) {
                return new Bucket(now);
            }
            // Reset window if expired
            if (now - existing.windowStartEpoch >= windowSeconds) {
                existing.count.set(0);
                existing.windowStartEpoch = now;
            }
            return existing;
        });

        int current = bucket.count.incrementAndGet();
        return current <= maxRequests;
    }

    /**
     * Reset the rate limit bucket for a given key (e.g., on successful auth).
     *
     * @param key rate limit key to reset
     */
    public void reset(String key) {
        buckets.remove(key);
    }

    /**
     * Returns remaining seconds until bucket window resets, or 0 if not tracked.
     *
     * @param key rate limit key
     * @param windowSeconds window duration in seconds
     */
    public long secondsUntilReset(String key, long windowSeconds) {
        Bucket bucket = buckets.get(key);
        if (bucket == null) return 0;
        long elapsed = Instant.now().getEpochSecond() - bucket.windowStartEpoch;
        long remaining = windowSeconds - elapsed;
        return Math.max(0, remaining);
    }
}
