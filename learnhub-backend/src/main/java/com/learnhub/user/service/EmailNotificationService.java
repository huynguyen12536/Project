package com.learnhub.user.service;

import com.learnhub.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@learnhub.local}")
    private String fromEmail;

    @Value("${app.url.frontend:http://localhost:3000}")
    private String frontendUrl;

    public void sendVerificationEmail(User user, String token) {
        try {
            String verificationUrl = String.format("%s/verify-email?token=%s", frontendUrl, token);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("Verify Your LearnHub Email Address");
            message.setText(buildVerificationEmailBody(user.getFirstName(), verificationUrl));

            mailSender.send(message);
            log.info("Verification email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", user.getEmail(), e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    public void sendPasswordResetEmail(User user, String token) {
        try {
            String resetUrl = String.format("%s/reset-password?token=%s", frontendUrl, token);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("Reset Your LearnHub Password");
            message.setText(buildPasswordResetEmailBody(user.getFirstName(), resetUrl));

            mailSender.send(message);
            log.info("Password reset email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", user.getEmail(), e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    private String buildVerificationEmailBody(String firstName, String verificationUrl) {
        return String.format(
                "Hi %s,\n\n" +
                "Welcome to LearnHub! Please verify your email by clicking the link below:\n\n" +
                "%s\n\n" +
                "This link expires in 24 hours.\n\n" +
                "If you didn't create this account, you can safely ignore this email.\n\n" +
                "Best regards,\n" +
                "LearnHub Team",
                firstName != null ? firstName : "Learner",
                verificationUrl
        );
    }

    private String buildPasswordResetEmailBody(String firstName, String resetUrl) {
        return String.format(
                "Hi %s,\n\n" +
                "We received a request to reset your password. Click the link below to set a new password:\n\n" +
                "%s\n\n" +
                "This link expires in 24 hours.\n\n" +
                "If you didn't request this, you can safely ignore this email.\n\n" +
                "Best regards,\n" +
                "LearnHub Team",
                firstName != null ? firstName : "Learner",
                resetUrl
        );
    }
}
