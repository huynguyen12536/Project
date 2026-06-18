package com.learnhub.user.service;

import com.learnhub.common.exception.ResourceNotFoundException;
import com.learnhub.common.util.TokenProvider;
import com.learnhub.user.dto.request.PasswordResetConfirm;
import com.learnhub.user.dto.request.PasswordResetRequest;
import com.learnhub.user.dto.response.PasswordResetResponse;
import com.learnhub.user.exception.InvalidTokenException;
import com.learnhub.user.exception.PasswordResetException;
import com.learnhub.user.model.PasswordResetToken;
import com.learnhub.user.model.User;
import com.learnhub.user.repository.PasswordResetTokenRepository;
import com.learnhub.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private EmailNotificationService emailService;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private PasswordResetService passwordResetService;

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
                .emailVerified(true)
                .role("LEARNER")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testInitiatePasswordReset_Success() {
        PasswordResetRequest request = new PasswordResetRequest();
        request.setEmail("test@example.com");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(tokenProvider.generateResetToken(testUserId)).thenReturn(testToken);
        when(tokenProvider.hashToken(testToken)).thenReturn(testTokenHash);
        when(httpRequest.getHeader("User-Agent")).thenReturn("Test Agent");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(tokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PasswordResetResponse response = passwordResetService.initiatePasswordReset(request, httpRequest);

        assertTrue(response.getSuccess());
        verify(emailService, times(1)).sendPasswordResetEmail(testUser, testToken);
    }

    @Test
    void testInitiatePasswordReset_UserNotFound() {
        PasswordResetRequest request = new PasswordResetRequest();
        request.setEmail("nonexistent@example.com");

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> passwordResetService.initiatePasswordReset(request, httpRequest));
    }

    @Test
    void testConfirmPasswordReset_Success() {
        PasswordResetConfirm confirm = new PasswordResetConfirm();
        confirm.setToken(testToken);
        confirm.setNewPassword("NewPassword123!");
        confirm.setConfirmPassword("NewPassword123!");

        PasswordResetToken token = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .token(testToken)
                .tokenHash(testTokenHash)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .isUsed(false)
                .build();

        when(tokenProvider.hashToken(testToken)).thenReturn(testTokenHash);
        when(tokenRepository.findByTokenHash(testTokenHash)).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("encoded_new_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PasswordResetResponse response = passwordResetService.confirmPasswordReset(confirm);

        assertTrue(response.getSuccess());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testConfirmPasswordReset_MismatchedPasswords() {
        PasswordResetConfirm confirm = new PasswordResetConfirm();
        confirm.setToken(testToken);
        confirm.setNewPassword("NewPassword123!");
        confirm.setConfirmPassword("DifferentPassword123!");

        assertThrows(PasswordResetException.class,
                () -> passwordResetService.confirmPasswordReset(confirm));
    }

    @Test
    void testConfirmPasswordReset_InvalidToken() {
        PasswordResetConfirm confirm = new PasswordResetConfirm();
        confirm.setToken(testToken);
        confirm.setNewPassword("NewPassword123!");
        confirm.setConfirmPassword("NewPassword123!");

        when(tokenProvider.hashToken(testToken)).thenReturn(testTokenHash);
        when(tokenRepository.findByTokenHash(testTokenHash)).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class,
                () -> passwordResetService.confirmPasswordReset(confirm));
    }

    @Test
    void testConfirmPasswordReset_ExpiredToken() {
        PasswordResetConfirm confirm = new PasswordResetConfirm();
        confirm.setToken(testToken);
        confirm.setNewPassword("NewPassword123!");
        confirm.setConfirmPassword("NewPassword123!");

        PasswordResetToken token = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .token(testToken)
                .tokenHash(testTokenHash)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .isUsed(false)
                .build();

        when(tokenProvider.hashToken(testToken)).thenReturn(testTokenHash);
        when(tokenRepository.findByTokenHash(testTokenHash)).thenReturn(Optional.of(token));

        assertThrows(InvalidTokenException.class,
                () -> passwordResetService.confirmPasswordReset(confirm));
    }

    @Test
    void testIsTokenValid_True() {
        PasswordResetToken token = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .token(testToken)
                .tokenHash(testTokenHash)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .isUsed(false)
                .build();

        when(tokenProvider.hashToken(testToken)).thenReturn(testTokenHash);
        when(tokenRepository.findByTokenHash(testTokenHash)).thenReturn(Optional.of(token));

        boolean result = passwordResetService.isTokenValid(testToken);

        assertTrue(result);
    }

    @Test
    void testIsTokenValid_False() {
        when(tokenProvider.hashToken(testToken)).thenReturn(testTokenHash);
        when(tokenRepository.findByTokenHash(testTokenHash)).thenReturn(Optional.empty());

        boolean result = passwordResetService.isTokenValid(testToken);

        assertFalse(result);
    }
}
