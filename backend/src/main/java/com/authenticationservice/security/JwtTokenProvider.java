package com.authenticationservice.security;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.UUID;
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
    private static final String REFRESH_FAMILY_CLAIM = "ftid";
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
        return generateRefreshToken(user, null);
    }

    public String generateRefreshToken(User user, Integer rememberDays) {
        String familyId = UUID.randomUUID().toString();
        String tokenId = UUID.randomUUID().toString();
        return generateRefreshToken(user, rememberDays, familyId, tokenId);
    }

    public String generateRefreshToken(User user, Integer rememberDays, String familyId, String tokenId) {
        if (rememberDays == null) {
            return generateRefreshTokenWithTtl(user, jwtProperties.getRefreshExpiration(), familyId, tokenId, null);
        }
        int days = rememberDays > 0 ? rememberDays : DEFAULT_REMEMBER_DAYS;
        long expirationMs = TimeUnit.DAYS.toMillis(days);
        return generateRefreshTokenWithTtl(user, expirationMs, familyId, tokenId, days);
    }

    private String generateRefreshTokenWithTtl(User user, long expirationMs, String familyId, String tokenId, Integer rememberDays) {
        try {
            Date now = new Date();
            if (expirationMs <= 0) {
                throw new IllegalStateException("JWT refresh expiration is not set or is invalid: " + expirationMs);
            }
            Date expiry = new Date(now.getTime() + expirationMs);
            var builder = Jwts.builder();
            builder.subject(user.getEmail());
            builder.issuedAt(now);
            builder.expiration(expiry);
            if (tokenId != null && !tokenId.isBlank()) {
                builder.id(tokenId);
            }
            if (familyId != null && !familyId.isBlank()) {
                builder.claim(REFRESH_FAMILY_CLAIM, familyId);
            }
            if (rememberDays != null) {
                builder.claim(REMEMBER_DAYS_CLAIM, rememberDays);
            }
            builder.signWith(refreshKey);
            return builder.compact();
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
        Claims claims = parseRefreshClaims(refreshToken);
        return claims.getSubject();
    }

    public String getRefreshTokenId(String refreshToken) {
        try {
            return parseRefreshClaims(refreshToken).getId();
        } catch (Exception ex) {
            return null;
        }
    }

    public String getRefreshTokenFamilyId(String refreshToken) {
        try {
            return parseRefreshClaims(refreshToken).get(REFRESH_FAMILY_CLAIM, String.class);
        } catch (Exception ex) {
            return null;
        }
    }

    public Integer getRememberDaysFromRefresh(String refreshToken) {
        try {
            Claims claims = parseRefreshClaims(refreshToken);
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

    public LocalDateTime getRefreshTokenExpirationTime(String refreshToken) {
        try {
            Claims claims = parseRefreshClaims(refreshToken);
            Date expiration = claims.getExpiration();
            if (expiration == null) {
                return null;
            }
            return LocalDateTime.ofInstant(expiration.toInstant(), ZoneOffset.UTC);
        } catch (Exception ex) {
            return null;
        }
    }

    public long getRefreshTokenTtlSeconds(String refreshToken) {
        try {
            Claims claims = parseRefreshClaims(refreshToken);
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

    private Claims parseRefreshClaims(String refreshToken) {
        return refreshParser.parseSignedClaims(refreshToken).getPayload();
    }
}
