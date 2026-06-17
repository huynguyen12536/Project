package com.learnhub.user.repository;

import com.learnhub.user.model.AccountLockout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountLockoutRepository extends JpaRepository<AccountLockout, UUID> {

    Optional<AccountLockout> findByUserIdAndIsActiveTrue(UUID userId);

    List<AccountLockout> findByUserIdOrderByCreatedAtDesc(UUID userId);

    void deleteByLockedUntilBefore(LocalDateTime dateTime);

    @Query("SELECT a FROM AccountLockout a WHERE a.user.id = ?1 AND a.isActive = true AND a.lockedUntil > CURRENT_TIMESTAMP")
    Optional<AccountLockout> findActiveLockout(UUID userId);
}
