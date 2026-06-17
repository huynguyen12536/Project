package com.learnhub.user.controller;

import com.learnhub.common.util.AuthenticationUtil;
import com.learnhub.user.dto.request.VerifyEmailRequest;
import com.learnhub.user.dto.response.EmailVerificationResponse;
import com.learnhub.user.service.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/email")
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationController {

    private final EmailVerificationService emailService;
    private final AuthenticationUtil authenticationUtil;

    @PostMapping("/verify")
    public ResponseEntity<EmailVerificationResponse> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request) {
        EmailVerificationResponse response = emailService.verifyEmail(request.getToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-verification")
    @PreAuthorize("hasRole('LEARNER')")
    public ResponseEntity<Map<String, String>> resendVerificationEmail() {
        UUID userId = authenticationUtil.getCurrentUserId();
        emailService.resendVerificationEmail(userId);

        return ResponseEntity.ok(Map.of(
                "message", "Verification email has been resent to your email address"
        ));
    }
}
