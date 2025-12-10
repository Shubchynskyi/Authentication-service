package com.authenticationservice.service;

import com.authenticationservice.config.RateLimitConfig;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class RateLimitingService {

    private final RateLimitConfig rateLimitConfig;
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String key) {
        return resolveBucket(key, RateLimitType.AUTH);
    }

    public Bucket resolveBucket(String key, boolean isAdminPath) {
        return resolveBucket(key, isAdminPath ? RateLimitType.ADMIN : RateLimitType.AUTH);
    }

    public Bucket resolveResendBucket(String emailKey) {
        return resolveBucket("resend:" + emailKey, RateLimitType.RESEND);
    }

    private Bucket resolveBucket(String key, RateLimitType type) {
        return cache.computeIfAbsent(key, k -> newBucket(type));
    }

    private Bucket newBucket(RateLimitType type) {
        Supplier<Integer> capacitySupplier = () -> switch (type) {
            case ADMIN -> rateLimitConfig.getAdminPerMinute();
            case RESEND -> rateLimitConfig.getResendPerMinute();
            default -> rateLimitConfig.getAuthPerMinute();
        };
        int capacity = Math.max(1, capacitySupplier.get());
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, Duration.ofMinutes(1))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private enum RateLimitType {
        AUTH,
        ADMIN,
        RESEND
    }
}
