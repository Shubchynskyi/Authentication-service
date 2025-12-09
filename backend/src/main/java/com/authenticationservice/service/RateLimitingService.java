package com.authenticationservice.service;

import com.authenticationservice.config.RateLimitConfig;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RateLimitingService {

    private final RateLimitConfig rateLimitConfig;
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String key) {
        return resolveBucket(key, false);
    }

    public Bucket resolveBucket(String key, boolean isAdminPath) {
        return cache.computeIfAbsent(key, k -> newBucket(isAdminPath));
    }

    private Bucket newBucket(boolean isAdminPath) {
        // Values are loaded from configuration to allow easy runtime tuning
        int capacity = isAdminPath ? rateLimitConfig.getAdminPerMinute() : rateLimitConfig.getAuthPerMinute();
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, Duration.ofMinutes(1))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
