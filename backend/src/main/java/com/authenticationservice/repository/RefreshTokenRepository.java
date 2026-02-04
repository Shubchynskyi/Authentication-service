package com.authenticationservice.repository;

import com.authenticationservice.model.RefreshToken;
import com.authenticationservice.model.RefreshTokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByJtiHash(String jtiHash);

    @Modifying
    @Query("""
            UPDATE RefreshToken t
            SET t.status = :status
            WHERE t.family.id = :familyId AND t.status <> :status
            """)
    int updateStatusByFamily(@Param("familyId") String familyId,
                             @Param("status") RefreshTokenStatus status);

    @Modifying
    @Query("""
            UPDATE RefreshToken t
            SET t.status = :status
            WHERE t.user.id = :userId AND t.status <> :status
            """)
    int updateStatusByUser(@Param("userId") Long userId,
                           @Param("status") RefreshTokenStatus status);

    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff);
}
