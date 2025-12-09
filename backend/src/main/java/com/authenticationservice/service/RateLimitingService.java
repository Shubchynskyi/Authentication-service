package com.authenticationservice.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String key) {
        return resolveBucket(key, false);
    }

    public Bucket resolveBucket(String key, boolean isAdminPath) {
        return cache.computeIfAbsent(key, k -> newBucket(isAdminPath));
    }

    private Bucket newBucket(boolean isAdminPath) {
        // Admin endpoints: 60 requests per minute (1 per second average)
        // Auth endpoints: 10 requests per minute (more restricted for security)
        int capacity = isAdminPath ? 60 : 10;
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, Duration.ofMinutes(1))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
