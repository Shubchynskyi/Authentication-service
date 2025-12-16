package com.authenticationservice.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Filter for logging HTTP requests and responses with performance metrics.
 * Logs request details, response status, and execution time.
 * Separates slow requests (>threshold) for easier monitoring.
 */
@Slf4j
@Component
public class HttpRequestLoggingFilter extends OncePerRequestFilter {

    private static final List<String> SKIP_PATHS = Arrays.asList(
            "/actuator", "/health", "/favicon.ico"
    );

    @Value("${slow-request-threshold-ms:1000}")
    private long slowRequestThresholdMs;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        if (shouldSkip(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();
        
        // Wrap request and response to enable content caching for logging
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logRequestResponse(wrappedRequest, wrappedResponse, duration);
            wrappedResponse.copyBodyToResponse();
        }
    }

    private boolean shouldSkip(String requestUri) {
        if (requestUri == null) {
            return true;
        }
        return SKIP_PATHS.stream().anyMatch(requestUri::startsWith);
    }

    private void logRequestResponse(HttpServletRequest request, 
                                    ContentCachingResponseWrapper response, 
                                    long durationMs) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        int statusCode = response.getStatus();
        int contentLength = response.getContentSize();

        String fullPath = queryString != null ? uri + "?" + queryString : uri;

        if (durationMs > slowRequestThresholdMs) {
            log.warn("Slow request: {} {} - Status: {} - Duration: {}ms - Response size: {} bytes",
                    method, fullPath, statusCode, durationMs, contentLength);
        } else {
            log.debug("Request: {} {} - Status: {} - Duration: {}ms - Response size: {} bytes",
                    method, fullPath, statusCode, durationMs, contentLength);
        }
    }
}

