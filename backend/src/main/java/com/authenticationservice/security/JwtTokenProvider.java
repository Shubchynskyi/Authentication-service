package com.authenticationservice.security;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.authenticationservice.config.JwtProperties;
import com.authenticationservice.model.User;
import com.authenticationservice.model.Role;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {

    private static final String ROLES_CLAIM = "roles";
    private static final String REMEMBER_DAYS_CLAIM = "rememberDays";
    private static final int DEFAULT_REMEMBER_DAYS = 15;

    private final JwtProperties jwtProperties;
    private final SecretKey key;
    private final SecretKey refreshKey;
    private final JwtParser accessParser;
    private final JwtParser refreshParser;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getAccessSecret()));
        this.refreshKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getRefreshSecret()));
        this.accessParser = Jwts.parser().verifyWith(key).build();
        this.refreshParser = Jwts.parser().verifyWith(refreshKey).build();
    }

    public String generateAccessToken(User user) {
        try {
            Date now = new Date();
            long expiration = jwtProperties.getAccessExpiration();
            if (expiration <= 0) {
                throw new IllegalStateException("JWT access expiration is not set or is invalid: " + expiration);
            }
            Date expiry = new Date(now.getTime() + expiration);
            
            // Create a new ArrayList to avoid UnsupportedOperationException with Hibernate collections
            List<String> roles = new java.util.ArrayList<>();
            if (user.getRoles() != null) {
                for (Role role : user.getRoles()) {
                    roles.add(role.getName());
                }
            }

            var builder = Jwts.builder();
            builder.subject(user.getEmail());
            builder.issuedAt(now);
            builder.expiration(expiry);
            builder.claim(ROLES_CLAIM, roles);
            builder.signWith(key);
            return builder.compact();
        } catch (Exception e) {
            throw new RuntimeException("Error generating access token: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()), e);
        }
    }

    public String generateRefreshToken(User user) {
        try {
            Date now = new Date();
            long expiration = jwtProperties.getRefreshExpiration();
            if (expiration <= 0) {
                throw new IllegalStateException("JWT refresh expiration is not set or is invalid: " + expiration);
            }
            Date expiry = new Date(now.getTime() + expiration);
            return Jwts.builder()
                    .subject(user.getEmail())
                    .issuedAt(now)
                    .expiration(expiry)
                    .signWith(refreshKey)
                    .compact();
        } catch (Exception e) {
            throw new RuntimeException("Error generating refresh token: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()), e);
        }
    }

    public String generateRefreshToken(User user, Integer rememberDays) {
        if (rememberDays == null) {
            return generateRefreshToken(user);
        }
        int days = rememberDays > 0 ? rememberDays : DEFAULT_REMEMBER_DAYS;
        long expirationMs = TimeUnit.DAYS.toMillis(days);
        try {
            Date now = new Date();
            Date expiry = new Date(now.getTime() + expirationMs);
            return Jwts.builder()
                    .subject(user.getEmail())
                    .issuedAt(now)
                    .expiration(expiry)
                    .claim(REMEMBER_DAYS_CLAIM, days)
                    .signWith(refreshKey)
                    .compact();
        } catch (Exception e) {
            throw new RuntimeException("Error generating refresh token: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()), e);
        }
    }

    public boolean validateRefreshToken(String refreshToken) {
        try {
            refreshParser.parseSignedClaims(refreshToken);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public String getEmailFromRefresh(String refreshToken) {
        Claims claims = refreshParser.parseSignedClaims(refreshToken).getPayload();
        return claims.getSubject();
    }

    public Integer getRememberDaysFromRefresh(String refreshToken) {
        try {
            Claims claims = refreshParser.parseSignedClaims(refreshToken).getPayload();
            Object value = claims.get(REMEMBER_DAYS_CLAIM);
            if (value instanceof Integer i) {
                return i;
            }
            if (value instanceof Number n) {
                return n.intValue();
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    public long getRefreshTokenTtlSeconds(String refreshToken) {
        try {
            Claims claims = refreshParser.parseSignedClaims(refreshToken).getPayload();
            Date expiration = claims.getExpiration();
            if (expiration == null) {
                return 0;
            }
            long nowMillis = System.currentTimeMillis();
            long ttlMillis = expiration.getTime() - nowMillis;
            return Math.max(0, TimeUnit.MILLISECONDS.toSeconds(ttlMillis));
        } catch (Exception ex) {
            return 0;
        }
    }

    public boolean validateAccessToken(String accessToken) {
        try {
            accessParser.parseSignedClaims(accessToken);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public String getEmailFromAccess(String accessToken) {
        Claims claims = accessParser.parseSignedClaims(accessToken).getPayload();
        return claims.getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromAccess(String accessToken) {
        Claims claims = accessParser.parseSignedClaims(accessToken).getPayload();
        return claims.get(ROLES_CLAIM, List.class);
    }
}
