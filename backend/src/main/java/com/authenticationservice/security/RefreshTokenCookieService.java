package com.authenticationservice.security;

import com.authenticationservice.config.RefreshCookieProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookieService {

    private final RefreshCookieProperties properties;
    private final JwtTokenProvider jwtTokenProvider;

    public String getCookieName() {
        return properties.getName();
    }

    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        long maxAgeSeconds = jwtTokenProvider.getRefreshTokenTtlSeconds(refreshToken);
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(properties.getName(), refreshToken)
                .httpOnly(properties.isHttpOnly())
                .secure(properties.isSecure())
                .path(properties.getPath())
                .maxAge(Duration.ofSeconds(Math.max(0, maxAgeSeconds)))
                .sameSite(properties.getSameSite());

        if (properties.getDomain() != null && !properties.getDomain().isBlank()) {
            builder.domain(properties.getDomain());
        }

        return builder.build();
    }

    public ResponseCookie clearRefreshTokenCookie() {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(properties.getName(), "")
                .httpOnly(properties.isHttpOnly())
                .secure(properties.isSecure())
                .path(properties.getPath())
                .maxAge(Duration.ZERO)
                .sameSite(properties.getSameSite());

        if (properties.getDomain() != null && !properties.getDomain().isBlank()) {
            builder.domain(properties.getDomain());
        }

        return builder.build();
    }
}
