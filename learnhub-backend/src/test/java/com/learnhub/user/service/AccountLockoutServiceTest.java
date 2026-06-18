package com.learnhub.user.service;

import com.learnhub.common.exception.ResourceNotFoundException;
import com.learnhub.user.exception.AccountLockedException;
import com.learnhub.user.model.AccountLockout;
import com.learnhub.user.model.User;
import com.learnhub.user.repository.AccountLockoutRepository;
import com.learnhub.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountLockoutServiceTest {

    @Mock
    private AccountLockoutRepository lockoutRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccountLockoutService accountLockoutService;

    private UUID testUserId;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = User.builder()
                .id(testUserId)
                .email("test@example.com")
                .passwordHash("hashed_password")
                .emailVerified(true)
                .role("LEARNER")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testRecordFailedLoginAttempt_FirstAttempt() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(lockoutRepository.findActiveLockout(testUserId)).thenReturn(Optional.empty());
        when(lockoutRepository.findByUserIdAndIsActiveTrue(testUserId)).thenReturn(Optional.empty());
        when(lockoutRepository.save(any(AccountLockout.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        accountLockoutService.recordFailedLoginAttempt(testUserId);

        verify(lockoutRepository, times(1)).save(any(AccountLockout.class));
    }

    @Test
    void testRecordFailedLoginAttempt_MultipleAttempts() {
        AccountLockout existingLockout = AccountLockout.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .failedAttempts(2)
                .lockedUntil(LocalDateTime.now().plusMinutes(30))
                .isActive(true)
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(lockoutRepository.findActiveLockout(testUserId)).thenReturn(Optional.of(existingLockout));
        when(lockoutRepository.save(any(AccountLockout.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        accountLockoutService.recordFailedLoginAttempt(testUserId);

        verify(lockoutRepository, times(1)).save(any(AccountLockout.class));
    }

    @Test
    void testRecordSuccessfulLogin_ReleasesLockout() {
        AccountLockout lockout = AccountLockout.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .failedAttempts(5)
                .lockedUntil(LocalDateTime.now().plusMinutes(30))
                .isActive(true)
                .build();

        when(lockoutRepository.findByUserIdAndIsActiveTrue(testUserId)).thenReturn(Optional.of(lockout));
        when(lockoutRepository.save(any(AccountLockout.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        accountLockoutService.recordSuccessfulLogin(testUserId);

        verify(lockoutRepository, times(1)).save(any(AccountLockout.class));
    }

    @Test
    void testIsAccountLocked_True() {
        AccountLockout lockout = AccountLockout.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .failedAttempts(5)
                .lockedUntil(LocalDateTime.now().plusMinutes(30))
                .isActive(true)
                .build();

        when(lockoutRepository.findActiveLockout(testUserId)).thenReturn(Optional.of(lockout));

        boolean result = accountLockoutService.isAccountLocked(testUserId);

        assertTrue(result);
    }

    @Test
    void testIsAccountLocked_False() {
        when(lockoutRepository.findActiveLockout(testUserId)).thenReturn(Optional.empty());

        boolean result = accountLockoutService.isAccountLocked(testUserId);

        assertFalse(result);
    }

    @Test
    void testGetActiveLockout_Success() {
        AccountLockout lockout = AccountLockout.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .failedAttempts(5)
                .lockedUntil(LocalDateTime.now().plusMinutes(30))
                .isActive(true)
                .build();

        when(lockoutRepository.findActiveLockout(testUserId)).thenReturn(Optional.of(lockout));

        AccountLockout result = accountLockoutService.getActiveLockout(testUserId);

        assertNotNull(result);
        assertEquals(5, result.getFailedAttempts());
    }

    @Test
    void testGetActiveLockout_NotFound() {
        when(lockoutRepository.findActiveLockout(testUserId)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> accountLockoutService.getActiveLockout(testUserId));
    }

    @Test
    void testGetRemainingLockoutMinutes_Success() {
        AccountLockout lockout = AccountLockout.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .failedAttempts(5)
                .lockedUntil(LocalDateTime.now().plusMinutes(20))
                .isActive(true)
                .build();

        when(lockoutRepository.findActiveLockout(testUserId)).thenReturn(Optional.of(lockout));

        Long remainingMinutes = accountLockoutService.getRemainingLockoutMinutes(testUserId);

        assertNotNull(remainingMinutes);
        assertTrue(remainingMinutes > 0);
        assertTrue(remainingMinutes <= 20);
    }

    @Test
    void testCheckAndThrowIfLocked_Locked() {
        AccountLockout lockout = AccountLockout.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .failedAttempts(5)
                .lockedUntil(LocalDateTime.now().plusMinutes(30))
                .isActive(true)
                .build();

        when(lockoutRepository.findActiveLockout(testUserId)).thenReturn(Optional.of(lockout));

        assertThrows(AccountLockedException.class, () -> accountLockoutService.checkAndThrowIfLocked(testUserId));
    }

    @Test
    void testCheckAndThrowIfLocked_NotLocked() {
        when(lockoutRepository.findActiveLockout(testUserId)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> accountLockoutService.checkAndThrowIfLocked(testUserId));
    }
}
