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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailNotificationService emailService;
    private final TokenProvider tokenProvider;

    @Value("${app.token.verification.expiry-hours:24}")
    private long verificationTokenExpiryHours;

    @Transactional
    public void sendVerificationEmail(User user) {
        // Invalidate previous token if exists
        tokenRepository.findByUserId(user.getId())
                .ifPresent(token -> {
                    token.setIsExpired(true);
                    tokenRepository.save(token);
                });

        // Generate new token
        String token = tokenProvider.generateVerificationToken(user.getId());
        String tokenHash = tokenProvider.hashToken(token);

        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .user(user)
                .token(token)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusHours(verificationTokenExpiryHours))
                .isExpired(false)
                .build();

        tokenRepository.save(verificationToken);

        // Send email
        emailService.sendVerificationEmail(user, token);
        log.info("Verification email sent to user: {}", user.getId());
    }

    @Transactional
    public EmailVerificationResponse verifyEmail(String token) {
        String tokenHash = tokenProvider.hashToken(token);

        EmailVerificationToken verificationToken = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired verification token"));

        if (!verificationToken.isValid()) {
            throw new InvalidTokenException("Verification token has expired or been used");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);

        verificationToken.setVerifiedAt(LocalDateTime.now());
        verificationToken.setIsExpired(true);
        tokenRepository.save(verificationToken);

        log.info("Email verified for user: {}", user.getId());
        return EmailVerificationResponse.success();
    }

    @Transactional
    public void resendVerificationEmail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.isEmailVerified()) {
            throw new EmailAlreadyVerifiedException("Email is already verified");
        }

        sendVerificationEmail(user);
    }

    @Transactional(readOnly = true)
    public boolean isTokenValid(String token) {
        String tokenHash = tokenProvider.hashToken(token);
        return tokenRepository.findByTokenHash(tokenHash)
                .map(EmailVerificationToken::isValid)
                .orElse(false);
    }
}
