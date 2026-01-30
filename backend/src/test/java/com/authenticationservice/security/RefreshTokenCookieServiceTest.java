package com.authenticationservice.security;

import com.authenticationservice.config.RefreshCookieProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenCookieService Tests")
class RefreshTokenCookieServiceTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private RefreshTokenCookieService refreshTokenCookieService;

    @BeforeEach
    void setUp() {
        RefreshCookieProperties properties = new RefreshCookieProperties();
        properties.setName("refreshToken");
        properties.setPath("/api/auth");
        properties.setSecure(true);
        properties.setSameSite("Strict");
        properties.setDomain("example.com");
        properties.setHttpOnly(true);

        refreshTokenCookieService = new RefreshTokenCookieService(properties, jwtTokenProvider);
    }

    @Test
    @DisplayName("Should build refresh token cookie with configured attributes")
    void createRefreshTokenCookie_shouldApplyProperties() {
        when(jwtTokenProvider.getRefreshTokenTtlSeconds("token")).thenReturn(3600L);

        ResponseCookie cookie = refreshTokenCookieService.createRefreshTokenCookie("token");

        assertEquals("refreshToken", cookie.getName());
        assertEquals("token", cookie.getValue());
        assertEquals("/api/auth", cookie.getPath());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.isSecure());
        assertEquals("Strict", cookie.getSameSite());
        assertEquals(Duration.ofSeconds(3600), cookie.getMaxAge());
        assertEquals("example.com", cookie.getDomain());
    }

    @Test
    @DisplayName("Should use zero max-age when refresh token is expired")
    void createRefreshTokenCookie_shouldUseZeroMaxAge_whenExpired() {
        when(jwtTokenProvider.getRefreshTokenTtlSeconds("expired-token")).thenReturn(0L);

        ResponseCookie cookie = refreshTokenCookieService.createRefreshTokenCookie("expired-token");

        assertEquals(Duration.ZERO, cookie.getMaxAge());
    }

    @Test
    @DisplayName("Should clear refresh cookie with zero max-age")
    void clearRefreshTokenCookie_shouldExpireCookie() {
        ResponseCookie cookie = refreshTokenCookieService.clearRefreshTokenCookie();

        assertEquals("refreshToken", cookie.getName());
        assertEquals("", cookie.getValue());
        assertEquals("/api/auth", cookie.getPath());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.isSecure());
        assertEquals("Strict", cookie.getSameSite());
        assertEquals(Duration.ZERO, cookie.getMaxAge());
        assertEquals("example.com", cookie.getDomain());
    }
}
