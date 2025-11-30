package com.authenticationservice.security;

import com.authenticationservice.service.RateLimitingService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitingFilter Tests")
class RateLimitingFilterTest {

    @Mock
    private RateLimitingService rateLimitingService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Bucket bucket;

    @InjectMocks
    private RateLimitingFilter rateLimitingFilter;


    @Test
    @DisplayName("Should allow request when under limit")
    void doFilterInternal_shouldAllowRequest_whenUnderLimit() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(rateLimitingService.resolveBucket("192.168.1.1")).thenReturn(bucket);
        ConsumptionProbe probe = mock(ConsumptionProbe.class);
        when(probe.isConsumed()).thenReturn(true);
        when(probe.getRemainingTokens()).thenReturn(5L);
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);

        // Act
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(response).addHeader("X-Rate-Limit-Remaining", "5");
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    @DisplayName("Should block request when over limit")
    void doFilterInternal_shouldBlockRequest_whenOverLimit() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(rateLimitingService.resolveBucket("192.168.1.1")).thenReturn(bucket);
        ConsumptionProbe probe = mock(ConsumptionProbe.class);
        when(probe.isConsumed()).thenReturn(false);
        when(probe.getNanosToWaitForRefill()).thenReturn(30_000_000_000L); // 30 seconds
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);

        // Act
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain, never()).doFilter(any(), any());
        verify(response).addHeader("X-Rate-Limit-Retry-After-Seconds", "30");
        verify(response).sendError(429, "Too many requests");
    }

    @Test
    @DisplayName("Should return 429 when over limit")
    void doFilterInternal_shouldReturn429_whenOverLimit() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(rateLimitingService.resolveBucket("192.168.1.1")).thenReturn(bucket);
        ConsumptionProbe probe = mock(ConsumptionProbe.class);
        when(probe.isConsumed()).thenReturn(false);
        when(probe.getNanosToWaitForRefill()).thenReturn(60_000_000_000L);
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);

        // Act
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).sendError(eq(429), eq("Too many requests"));
    }

    @Test
    @DisplayName("Should add rate limit headers when allowed")
    void doFilterInternal_shouldAddRateLimitHeaders_whenAllowed() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(rateLimitingService.resolveBucket("192.168.1.1")).thenReturn(bucket);
        ConsumptionProbe probe = mock(ConsumptionProbe.class);
        when(probe.isConsumed()).thenReturn(true);
        when(probe.getRemainingTokens()).thenReturn(9L);
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);

        // Act
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).addHeader("X-Rate-Limit-Remaining", "9");
    }

    @Test
    @DisplayName("Should add retry after header when blocked")
    void doFilterInternal_shouldAddRetryAfterHeader_whenBlocked() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(rateLimitingService.resolveBucket("192.168.1.1")).thenReturn(bucket);
        ConsumptionProbe probe = mock(ConsumptionProbe.class);
        when(probe.isConsumed()).thenReturn(false);
        when(probe.getNanosToWaitForRefill()).thenReturn(45_000_000_000L); // 45 seconds
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);

        // Act
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).addHeader("X-Rate-Limit-Retry-After-Seconds", "45");
    }

    @Test
    @DisplayName("Should skip filter when not auth endpoint")
    void doFilterInternal_shouldSkipFilter_whenNotAuthEndpoint() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/protected/profile");
        doNothing().when(filterChain).doFilter(request, response);

        // Act
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain, times(1)).doFilter(request, response);
        verify(rateLimitingService, never()).resolveBucket(anyString());
        verify(response, never()).addHeader(anyString(), anyString());
        verify(response, never()).sendError(anyInt(), anyString());
    }
}

