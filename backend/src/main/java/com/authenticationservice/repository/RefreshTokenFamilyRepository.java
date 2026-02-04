package com.authenticationservice.repository;

import com.authenticationservice.model.RefreshTokenFamily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RefreshTokenFamilyRepository extends JpaRepository<RefreshTokenFamily, String> {

    List<RefreshTokenFamily> findByUserIdAndRevokedAtIsNullOrderByCreatedAtAsc(Long userId);

    @Modifying
    @Query("""
            UPDATE RefreshTokenFamily f
            SET f.revokedAt = :revokedAt, f.revokedReason = :reason
            WHERE f.id = :familyId AND f.revokedAt IS NULL
            """)
    int revokeFamily(@Param("familyId") String familyId,
                     @Param("revokedAt") LocalDateTime revokedAt,
                     @Param("reason") String reason);

    @Modifying
    @Query("""
            UPDATE RefreshTokenFamily f
            SET f.revokedAt = :revokedAt, f.revokedReason = :reason
            WHERE f.user.id = :userId AND f.revokedAt IS NULL
            """)
    int revokeAllForUser(@Param("userId") Long userId,
                         @Param("revokedAt") LocalDateTime revokedAt,
                         @Param("reason") String reason);
}
