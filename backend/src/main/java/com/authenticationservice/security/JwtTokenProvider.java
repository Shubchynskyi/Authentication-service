package com.authenticationservice.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.authenticationservice.config.JwtProperties;
import com.authenticationservice.model.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {

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
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getAccessExpiration());
        return Jwts.builder()
                .subject(user.getEmail())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getRefreshExpiration());
        return Jwts.builder()
                .subject(user.getEmail())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(refreshKey)
                .compact();
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
}