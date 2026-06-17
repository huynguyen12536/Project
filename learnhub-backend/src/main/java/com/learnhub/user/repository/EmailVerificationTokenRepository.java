package com.learnhub.user.repository;

import com.learnhub.user.model.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByToken(String token);

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    Optional<EmailVerificationToken> findByUserId(UUID userId);

    void deleteByExpiresAtBefore(LocalDateTime dateTime);

    void deleteByUserId(UUID userId);

    @Query("SELECT t FROM EmailVerificationToken t WHERE t.user.id = ?1 AND t.isExpired = false")
    Optional<EmailVerificationToken> findActiveTokenByUserId(UUID userId);
}
