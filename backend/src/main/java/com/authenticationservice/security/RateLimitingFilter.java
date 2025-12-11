package com.authenticationservice.security;

import com.authenticationservice.constants.MessageConstants;
import com.authenticationservice.service.RateLimitingService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitingService rateLimitingService;
    private static final List<String> SKIPPED_AUTH_PATH_PREFIXES = List.of(
            "/api/auth/verify"
    );

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        boolean isAuthPath = path.startsWith("/api/auth/");
        boolean isAdminPath = path.startsWith("/api/admin/");

        // Skip rate limiting for verification-related endpoints to avoid blocking users
        if (isAuthPath && shouldSkip(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        if (!isAuthPath && !isAdminPath) {
            filterChain.doFilter(request, response);
            return;
        }

        // Security: Extract real IP address considering proxies/load balancers
        String ip = getClientIpAddress(request);
        // Use different bucket keys for different paths to apply different rate limits
        String bucketKey = isAdminPath ? "admin:" + ip : "auth:" + ip;
        Bucket bucket = rateLimitingService.resolveBucket(bucketKey, isAdminPath);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
            response.getWriter().write(String.format(
                    "{\"error\":\"%s\",\"retryAfter\":%d}",
                    MessageConstants.TOO_MANY_REQUESTS,
                    waitForRefill));
            response.getWriter().flush();
            // Do NOT call filterChain.doFilter() - stop processing here
        }
    }

    private boolean shouldSkip(String path) {
        return SKIPPED_AUTH_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    /**
     * Extract client IP address from request, considering X-Forwarded-For header
     * for requests behind proxies/load balancers
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            String[] ips = xForwardedFor.split(",");
            return ips[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp.trim();
        }
        
        return request.getRemoteAddr();
    }
}
