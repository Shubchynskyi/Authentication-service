package com.authenticationservice.logging;

import com.authenticationservice.constants.LoggingConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.util.UUID;

/**
 * Adds request/trace identifiers to MDC and response headers to enable
 * request correlation across services.
 */
@Component
public class RequestCorrelationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String headerTraceId = request.getHeader(LoggingConstants.TRACE_ID_HEADER);
        String correlationId = request.getHeader(LoggingConstants.CORRELATION_ID_HEADER);

        String traceId = resolveTraceId(headerTraceId, correlationId);
        MDC.put(LoggingConstants.TRACE_ID_MDC_KEY, traceId);

        MDC.put(LoggingConstants.CLIENT_IP_MDC_KEY, getClientIpAddress(request));
        MDC.put(LoggingConstants.HTTP_METHOD_MDC_KEY, request.getMethod());
        MDC.put(LoggingConstants.REQUEST_PATH_MDC_KEY, request.getRequestURI());
        
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && !userAgent.isBlank()) {
            // Truncate user agent if too long to avoid excessive log size
            MDC.put(LoggingConstants.USER_AGENT_MDC_KEY, 
                    userAgent.length() > 200 ? userAgent.substring(0, 200) : userAgent);
        }

        String userEmail = resolveUserEmail();
        if (userEmail != null) {
            MDC.put(LoggingConstants.USER_EMAIL_MDC_KEY, userEmail);
        }

        response.setHeader(LoggingConstants.TRACE_ID_HEADER, traceId);
        if (StringUtils.hasText(correlationId)) {
            response.setHeader(LoggingConstants.CORRELATION_ID_HEADER, correlationId);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private String resolveTraceId(String headerTraceId, String correlationId) {
        if (StringUtils.hasText(headerTraceId)) {
            return headerTraceId;
        }
        if (StringUtils.hasText(correlationId)) {
            return correlationId;
        }
        return UUID.randomUUID().toString();
    }

    private String resolveUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        String name = authentication.getName();
        return (name != null && !name.equalsIgnoreCase("anonymousUser")) ? name : null;
    }

    /**
     * Extract client IP address from request, considering X-Forwarded-For header
     * for requests behind proxies/load balancers.
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

