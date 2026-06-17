package com.learnhub.user.repository;

import com.learnhub.user.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    List<PasswordResetToken> findByUserId(UUID userId);

    void deleteByExpiresAtBefore(LocalDateTime dateTime);

    void deleteByUserIdAndIsUsedFalse(UUID userId);

    void deleteByUserIdAndIsUsedTrue(UUID userId);

    @Query("SELECT t FROM PasswordResetToken t WHERE t.user.id = ?1 AND t.isUsed = false AND t.expiresAt > CURRENT_TIMESTAMP ORDER BY t.createdAt DESC")
    Optional<PasswordResetToken> findLatestActiveToken(UUID userId);
}
