package com.authenticationservice.service;

import com.authenticationservice.config.RefreshTokenRotationProperties;
import com.authenticationservice.constants.MessageConstants;
import com.authenticationservice.model.RefreshToken;
import com.authenticationservice.model.RefreshTokenFamily;
import com.authenticationservice.model.RefreshTokenStatus;
import com.authenticationservice.model.User;
import com.authenticationservice.repository.RefreshTokenFamilyRepository;
import com.authenticationservice.repository.RefreshTokenRepository;
import com.authenticationservice.security.JwtTokenProvider;
import com.authenticationservice.util.LoggingSanitizer;
import com.authenticationservice.util.TokenHashing;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenRotationService {

    private static final String REVOKE_REASON_REUSE = "refresh_token_reuse_detected";
    private static final String REVOKE_REASON_LOGOUT = "logout";
    private static final String REVOKE_REASON_PASSWORD_CHANGE = "password_changed";
    private static final String REVOKE_REASON_ACCOUNT_BLOCKED = "account_blocked";
    private static final String REVOKE_REASON_ACCOUNT_DISABLED = "account_disabled";
    private static final String REVOKE_REASON_MAX_FAMILIES = "max_families_exceeded";

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenFamilyRepository refreshTokenFamilyRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRotationProperties properties;
    private final EntityManager entityManager;

    public String issueRefreshToken(User user, Integer rememberDays, String ipAddress, String userAgent) {
        if (!properties.isEnabled()) {
            return rememberDays != null
                    ? jwtTokenProvider.generateRefreshToken(user, rememberDays)
                    : jwtTokenProvider.generateRefreshToken(user);
        }

        String familyId = UUID.randomUUID().toString();
        RefreshTokenFamily family = new RefreshTokenFamily();
        family.setId(familyId);
        User managedUser = ensureManagedUser(user);
        family.setUser(managedUser);
        family.setCreatedAt(LocalDateTime.now());
        refreshTokenFamilyRepository.save(family);

        String refreshToken = createAndStoreRefreshToken(managedUser, family, rememberDays, ipAddress, userAgent);
        enforceMaxFamilies(managedUser.getId());
        cleanupExpiredTokensIfEnabled();
        return refreshToken;
    }

    public String rotateRefreshToken(String refreshToken, User user, String ipAddress, String userAgent) {
        if (!properties.isEnabled()) {
            Integer rememberDays = jwtTokenProvider.getRememberDaysFromRefresh(refreshToken);
            return rememberDays != null
                    ? jwtTokenProvider.generateRefreshToken(user, rememberDays)
                    : jwtTokenProvider.generateRefreshToken(user);
        }

        User managedUser = ensureManagedUser(user);

        String tokenId = jwtTokenProvider.getRefreshTokenId(refreshToken);
        String familyId = jwtTokenProvider.getRefreshTokenFamilyId(refreshToken);
        if (tokenId == null || familyId == null) {
            log.warn("Refresh token missing required claims for user {}", maskEmail(user.getEmail()));
            throw new RuntimeException(MessageConstants.INVALID_REFRESH_TOKEN);
        }

        String tokenHash = TokenHashing.sha256Hex(tokenId);
        RefreshToken existing = refreshTokenRepository.findByJtiHash(tokenHash).orElse(null);
        if (existing == null) {
            handleReuseDetected(familyId, user);
            throw new RuntimeException(MessageConstants.INVALID_REFRESH_TOKEN);
        }

        if (!existing.getUser().getId().equals(user.getId())) {
            handleReuseDetected(familyId, user);
            throw new RuntimeException(MessageConstants.INVALID_REFRESH_TOKEN);
        }

        RefreshTokenFamily family = existing.getFamily();
        if (family.getRevokedAt() != null) {
            throw new RuntimeException(MessageConstants.INVALID_REFRESH_TOKEN);
        }

        if (existing.getStatus() != RefreshTokenStatus.ACTIVE) {
            handleReuseDetected(familyId, user);
            throw new RuntimeException(MessageConstants.INVALID_REFRESH_TOKEN);
        }

        Integer rememberDays = jwtTokenProvider.getRememberDaysFromRefresh(refreshToken);
        String newTokenId = UUID.randomUUID().toString();
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(managedUser, rememberDays, familyId, newTokenId);
        String newTokenHash = TokenHashing.sha256Hex(newTokenId);

        existing.setStatus(RefreshTokenStatus.ROTATED);
        existing.setReplacedByJtiHash(newTokenHash);
        refreshTokenRepository.save(existing);

        family.setLastUsedAt(LocalDateTime.now());
        refreshTokenFamilyRepository.save(family);

        RefreshToken next = buildRefreshTokenEntity(managedUser, family, newTokenHash, ipAddress, userAgent, newRefreshToken);
        refreshTokenRepository.save(next);

        cleanupExpiredTokensIfEnabled();
        return newRefreshToken;
    }

    public void revokeByRefreshToken(String refreshToken) {
        if (!properties.isEnabled()) {
            return;
        }
        String familyId = jwtTokenProvider.getRefreshTokenFamilyId(refreshToken);
        if (familyId == null) {
            return;
        }
        revokeFamily(familyId, REVOKE_REASON_LOGOUT);
    }

    public void revokeAllForUser(Long userId, String reason) {
        if (!properties.isEnabled()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        refreshTokenFamilyRepository.revokeAllForUser(userId, now, reason);
        refreshTokenRepository.updateStatusByUser(userId, RefreshTokenStatus.REVOKED);
    }

    public void revokeForPasswordChange(Long userId) {
        revokeAllForUser(userId, REVOKE_REASON_PASSWORD_CHANGE);
    }

    public void revokeForAccountBlocked(Long userId) {
        revokeAllForUser(userId, REVOKE_REASON_ACCOUNT_BLOCKED);
    }

    public void revokeForAccountDisabled(Long userId) {
        revokeAllForUser(userId, REVOKE_REASON_ACCOUNT_DISABLED);
    }

    private void handleReuseDetected(String familyId, User user) {
        if (!properties.isRevokeOnReuse()) {
            return;
        }
        log.warn("Refresh token reuse detected for user {}", maskEmail(user.getEmail()));
        revokeFamily(familyId, REVOKE_REASON_REUSE);
    }

    private void revokeFamily(String familyId, String reason) {
        if (familyId == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        refreshTokenFamilyRepository.revokeFamily(familyId, now, reason);
        refreshTokenRepository.updateStatusByFamily(familyId, RefreshTokenStatus.REVOKED);
    }

    private String createAndStoreRefreshToken(User user, RefreshTokenFamily family, Integer rememberDays,
                                              String ipAddress, String userAgent) {
        String tokenId = UUID.randomUUID().toString();
        String refreshToken = jwtTokenProvider.generateRefreshToken(user, rememberDays, family.getId(), tokenId);
        String tokenHash = TokenHashing.sha256Hex(tokenId);
        RefreshToken entity = buildRefreshTokenEntity(user, family, tokenHash, ipAddress, userAgent, refreshToken);
        refreshTokenRepository.save(entity);
        return refreshToken;
    }

    private RefreshToken buildRefreshTokenEntity(User user, RefreshTokenFamily family, String tokenHash,
                                                 String ipAddress, String userAgent, String refreshToken) {
        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setFamily(family);
        entity.setJtiHash(tokenHash);
        entity.setIssuedAt(LocalDateTime.now());
        LocalDateTime expiresAt = jwtTokenProvider.getRefreshTokenExpirationTime(refreshToken);
        if (expiresAt == null) {
            long ttlSeconds = jwtTokenProvider.getRefreshTokenTtlSeconds(refreshToken);
            expiresAt = LocalDateTime.now().plusSeconds(Math.max(0, ttlSeconds));
        }
        entity.setExpiresAt(expiresAt);
        entity.setStatus(RefreshTokenStatus.ACTIVE);
        entity.setIpAddress(ipAddress);
        entity.setUserAgent(userAgent);
        return entity;
    }

    private void enforceMaxFamilies(Long userId) {
        int maxFamilies = properties.getMaxFamiliesPerUser();
        if (maxFamilies <= 0) {
            return;
        }
        List<RefreshTokenFamily> activeFamilies = refreshTokenFamilyRepository
                .findByUserIdAndRevokedAtIsNullOrderByCreatedAtAsc(userId);
        int overflow = activeFamilies.size() - maxFamilies;
        if (overflow <= 0) {
            return;
        }
        for (int i = 0; i < overflow; i++) {
            RefreshTokenFamily family = activeFamilies.get(i);
            revokeFamily(family.getId(), REVOKE_REASON_MAX_FAMILIES);
        }
    }

    private void cleanupExpiredTokensIfEnabled() {
        if (!properties.isCleanupEnabled()) {
            return;
        }
        int days = Math.max(0, properties.getCleanupExpiredAfterDays());
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        refreshTokenRepository.deleteExpiredBefore(cutoff);
    }

    private String maskEmail(String email) {
        return LoggingSanitizer.maskEmail(email);
    }

    private User ensureManagedUser(User user) {
        if (user == null || user.getId() == null) {
            throw new RuntimeException(MessageConstants.INVALID_REFRESH_TOKEN);
        }
        if (entityManager.contains(user)) {
            return user;
        }
        return entityManager.getReference(User.class, user.getId());
    }
}
