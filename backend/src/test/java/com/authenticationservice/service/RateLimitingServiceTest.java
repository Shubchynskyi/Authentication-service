package com.authenticationservice.service;

import com.authenticationservice.config.RateLimitConfig;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RateLimitingService Tests")
class RateLimitingServiceTest {

    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        RateLimitConfig rateLimitConfig = new RateLimitConfig();
        rateLimitConfig.setAdminPerMinute(300);
        rateLimitConfig.setAuthPerMinute(10); // Keep low for fast tests
        rateLimitingService = new RateLimitingService(rateLimitConfig);
    }

    @Test
    @DisplayName("Should create new bucket when key not exists")
    void resolveBucket_shouldCreateNewBucket_whenKeyNotExists() {
        // Arrange
        String ipAddress = "192.168.1.1";

        // Act
        Bucket bucket = rateLimitingService.resolveBucket(ipAddress);

        // Assert
        assertNotNull(bucket);
    }

    @Test
    @DisplayName("Should return same bucket when key exists")
    void resolveBucket_shouldReturnSameBucket_whenKeyExists() {
        // Arrange
        String ipAddress = "192.168.1.1";

        // Act
        Bucket bucket1 = rateLimitingService.resolveBucket(ipAddress);
        Bucket bucket2 = rateLimitingService.resolveBucket(ipAddress);

        // Assert
        assertSame(bucket1, bucket2);
    }

    @Test
    @DisplayName("Should allow configured requests per minute for auth path")
    void resolveBucket_shouldRespectConfiguredAuthLimit() {
        // Arrange
        String ipAddress = "192.168.1.1";
        Bucket bucket = rateLimitingService.resolveBucket(ipAddress);

        // Act & Assert - should allow configured number of requests
        for (int i = 0; i < 10; i++) {
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            assertTrue(probe.isConsumed(), "Request " + (i + 1) + " should be allowed");
        }

        // Next request should be blocked
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        assertFalse(probe.isConsumed(), "Request beyond limit should be blocked");
    }

    @Test
    @DisplayName("Should create different buckets for different IPs")
    void resolveBucket_shouldCreateDifferentBuckets_forDifferentIPs() {
        // Arrange
        String ip1 = "192.168.1.1";
        String ip2 = "192.168.1.2";

        // Act
        Bucket bucket1 = rateLimitingService.resolveBucket(ip1);
        Bucket bucket2 = rateLimitingService.resolveBucket(ip2);

        // Assert
        assertNotSame(bucket1, bucket2);
        
        // Both should allow requests independently
        ConsumptionProbe probe1 = bucket1.tryConsumeAndReturnRemaining(1);
        ConsumptionProbe probe2 = bucket2.tryConsumeAndReturnRemaining(1);
        assertTrue(probe1.isConsumed());
        assertTrue(probe2.isConsumed());
    }
}

