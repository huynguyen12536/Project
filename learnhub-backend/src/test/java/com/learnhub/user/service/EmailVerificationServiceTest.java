package com.learnhub.user.service;

import com.learnhub.common.exception.ResourceNotFoundException;
import com.learnhub.common.util.TokenProvider;
import com.learnhub.user.dto.response.EmailVerificationResponse;
import com.learnhub.user.exception.EmailAlreadyVerifiedException;
import com.learnhub.user.exception.InvalidTokenException;
import com.learnhub.user.model.EmailVerificationToken;
import com.learnhub.user.model.User;
import com.learnhub.user.repository.EmailVerificationTokenRepository;
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
class EmailVerificationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private EmailNotificationService emailService;

    @Mock
    private TokenProvider tokenProvider;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    private UUID testUserId;
    private User testUser;
    private String testToken;
    private String testTokenHash;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testToken = "valid_token_12345";
        testTokenHash = "hashed_token_12345";

        testUser = User.builder()
                .id(testUserId)
                .email("test@example.com")
                .passwordHash("hashed_password")
                .emailVerified(false)
                .role("LEARNER")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testSendVerificationEmail_Success() {
        when(tokenProvider.generateVerificationToken(testUserId)).thenReturn(testToken);
        when(tokenProvider.hashToken(testToken)).thenReturn(testTokenHash);
        when(tokenRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(tokenRepository.save(any(EmailVerificationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        emailVerificationService.sendVerificationEmail(testUser);

        verify(tokenRepository, times(1)).save(any(EmailVerificationToken.class));
        verify(emailService, times(1)).sendVerificationEmail(testUser, testToken);
    }

    @Test
    void testVerifyEmail_Success() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .token(testToken)
                .tokenHash(testTokenHash)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .isExpired(false)
                .build();

        when(tokenProvider.hashToken(testToken)).thenReturn(testTokenHash);
        when(tokenRepository.findByTokenHash(testTokenHash)).thenReturn(Optional.of(token));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenRepository.save(any(EmailVerificationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EmailVerificationResponse response = emailVerificationService.verifyEmail(testToken);

        assertTrue(response.getEmailVerified());
        assertNotNull(response.getEmailVerifiedAt());
        verify(userRepository, times(1)).save(any(User.class));
        verify(tokenRepository, times(1)).save(any(EmailVerificationToken.class));
    }

    @Test
    void testVerifyEmail_InvalidToken() {
        when(tokenProvider.hashToken(testToken)).thenReturn(testTokenHash);
        when(tokenRepository.findByTokenHash(testTokenHash)).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () -> emailVerificationService.verifyEmail(testToken));
    }

    @Test
    void testVerifyEmail_ExpiredToken() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .token(testToken)
                .tokenHash(testTokenHash)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .isExpired(false)
                .build();

        when(tokenProvider.hashToken(testToken)).thenReturn(testTokenHash);
        when(tokenRepository.findByTokenHash(testTokenHash)).thenReturn(Optional.of(token));

        assertThrows(InvalidTokenException.class, () -> emailVerificationService.verifyEmail(testToken));
    }

    @Test
    void testResendVerificationEmail_Success() {
        testUser.setEmailVerified(false);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(tokenProvider.generateVerificationToken(testUserId)).thenReturn(testToken);
        when(tokenProvider.hashToken(testToken)).thenReturn(testTokenHash);
        when(tokenRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(tokenRepository.save(any(EmailVerificationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        emailVerificationService.resendVerificationEmail(testUserId);

        verify(emailService, times(1)).sendVerificationEmail(any(User.class), eq(testToken));
    }

    @Test
    void testResendVerificationEmail_AlreadyVerified() {
        testUser.setEmailVerified(true);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        assertThrows(EmailAlreadyVerifiedException.class,
                () -> emailVerificationService.resendVerificationEmail(testUserId));
    }

    @Test
    void testIsTokenValid_True() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .token(testToken)
                .tokenHash(testTokenHash)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .isExpired(false)
                .build();

        when(tokenProvider.hashToken(testToken)).thenReturn(testTokenHash);
        when(tokenRepository.findByTokenHash(testTokenHash)).thenReturn(Optional.of(token));

        boolean result = emailVerificationService.isTokenValid(testToken);

        assertTrue(result);
    }

    @Test
    void testIsTokenValid_False() {
        when(tokenProvider.hashToken(testToken)).thenReturn(testTokenHash);
        when(tokenRepository.findByTokenHash(testTokenHash)).thenReturn(Optional.empty());

        boolean result = emailVerificationService.isTokenValid(testToken);

        assertFalse(result);
    }
}
