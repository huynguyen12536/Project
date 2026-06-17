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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailNotificationService emailService;
    private final TokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.token.reset.expiry-hours:24}")
    private long resetTokenExpiryHours;

    @Transactional
    public PasswordResetResponse initiatePasswordReset(PasswordResetRequest request,
                                                       HttpServletRequest httpRequest) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Generate reset token
        String token = tokenProvider.generateResetToken(user.getId());
        String tokenHash = tokenProvider.hashToken(token);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusHours(resetTokenExpiryHours))
                .isUsed(false)
                .ipAddress(getClientIpAddress(httpRequest))
                .userAgent(httpRequest.getHeader("User-Agent"))
                .build();

        tokenRepository.save(resetToken);

        // Send email
        emailService.sendPasswordResetEmail(user, token);
        log.info("Password reset email sent to user: {}", user.getId());

        return PasswordResetResponse.initiated("Password reset link has been sent to your email");
    }

    @Transactional
    public PasswordResetResponse confirmPasswordReset(PasswordResetConfirm request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new PasswordResetException("Passwords do not match");
        }

        String tokenHash = tokenProvider.hashToken(request.getToken());
        PasswordResetToken resetToken = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired reset token"));

        if (!resetToken.isValid()) {
            throw new InvalidTokenException("Reset token has expired or been used");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setLastPasswordChange(LocalDateTime.now());
        user.setPasswordChangeRequired(false);
        userRepository.save(user);

        resetToken.setUsedAt(LocalDateTime.now());
        resetToken.setIsUsed(true);
        tokenRepository.save(resetToken);

        log.info("Password reset completed for user: {}", user.getId());

        // Invalidate all other reset tokens for this user
        tokenRepository.deleteByUserIdAndIsUsedFalse(user.getId());

        return PasswordResetResponse.confirmed("Password has been reset successfully");
    }

    @Transactional(readOnly = true)
    public boolean isTokenValid(String token) {
        String tokenHash = tokenProvider.hashToken(token);
        return tokenRepository.findByTokenHash(tokenHash)
                .map(PasswordResetToken::isValid)
                .orElse(false);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
