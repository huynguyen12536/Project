package com.learnhub.user.service;

import com.learnhub.common.exception.ResourceNotFoundException;
import com.learnhub.user.exception.AccountLockedException;
import com.learnhub.user.model.AccountLockout;
import com.learnhub.user.model.User;
import com.learnhub.user.repository.AccountLockoutRepository;
import com.learnhub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountLockoutService {

    private final AccountLockoutRepository lockoutRepository;
    private final UserRepository userRepository;

    @Value("${app.security.max-failed-login-attempts:5}")
    private int maxFailedAttempts;

    @Value("${app.security.lockout-duration-minutes:30}")
    private long lockoutDurationMinutes;

    @Transactional
    public void recordFailedLoginAttempt(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Optional<AccountLockout> activeLockout = lockoutRepository.findActiveLockout(userId);

        if (activeLockout.isPresent()) {
            AccountLockout lockout = activeLockout.get();
            lockout.setFailedAttempts(lockout.getFailedAttempts() + 1);
            lockout.setLastFailedAttempt(LocalDateTime.now());
            lockoutRepository.save(lockout);
            log.warn("Failed login attempt recorded for user: {}", userId);
        } else {
            // Release expired lockout if any
            lockoutRepository.findByUserIdAndIsActiveTrue(userId)
                    .filter(lockout -> LocalDateTime.now().isAfter(lockout.getLockedUntil()))
                    .ifPresent(expiredLockout -> {
                        expiredLockout.setIsActive(false);
                        lockoutRepository.save(expiredLockout);
                    });

            // Create new lockout
            AccountLockout newLockout = AccountLockout.builder()
                    .user(user)
                    .failedAttempts(1)
                    .lastFailedAttempt(LocalDateTime.now())
                    .lockedUntil(LocalDateTime.now().plusMinutes(lockoutDurationMinutes))
                    .isActive(true)
                    .lockReason("Failed login attempts")
                    .build();

            lockoutRepository.save(newLockout);
            log.info("Failed login attempt recorded for user: {} (new lockout)", userId);
        }
    }

    @Transactional
    public void recordSuccessfulLogin(UUID userId) {
        lockoutRepository.findByUserIdAndIsActiveTrue(userId)
                .ifPresent(lockout -> {
                    lockout.setIsActive(false);
                    lockout.setReleasedAt(LocalDateTime.now());
                    lockoutRepository.save(lockout);
                    log.info("Account lockout released for user: {}", userId);
                });
    }

    @Transactional(readOnly = true)
    public boolean isAccountLocked(UUID userId) {
        return lockoutRepository.findActiveLockout(userId).isPresent();
    }

    @Transactional(readOnly = true)
    public AccountLockout getActiveLockout(UUID userId) {
        return lockoutRepository.findActiveLockout(userId)
                .orElseThrow(() -> new IllegalStateException("Account is not locked"));
    }

    @Transactional(readOnly = true)
    public Long getRemainingLockoutMinutes(UUID userId) {
        Optional<AccountLockout> activeLockout = lockoutRepository.findActiveLockout(userId);
        if (activeLockout.isEmpty()) {
            return 0L;
        }

        AccountLockout lockout = activeLockout.get();
        if (!lockout.isCurrentlyLocked()) {
            return 0L;
        }

        Duration duration = Duration.between(LocalDateTime.now(), lockout.getLockedUntil());
        return duration.toMinutes();
    }

    @Transactional
    public void checkAndThrowIfLocked(UUID userId) {
        Optional<AccountLockout> activeLockout = lockoutRepository.findActiveLockout(userId);
        if (activeLockout.isPresent()) {
            AccountLockout lockout = activeLockout.get();
            if (lockout.isCurrentlyLocked()) {
                Long remainingMinutes = getRemainingLockoutMinutes(userId);
                throw new AccountLockedException(
                        "Account is temporarily locked due to too many failed login attempts. Try again in " +
                                remainingMinutes + " minutes.",
                        remainingMinutes
                );
            }
        }
    }
}
