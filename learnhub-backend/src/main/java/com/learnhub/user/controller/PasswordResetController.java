package com.learnhub.user.controller;

import com.learnhub.user.dto.request.PasswordResetConfirm;
import com.learnhub.user.dto.request.PasswordResetRequest;
import com.learnhub.user.dto.response.PasswordResetResponse;
import com.learnhub.user.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/password")
@RequiredArgsConstructor
@Slf4j
public class PasswordResetController {

    private final PasswordResetService resetService;

    @PostMapping("/reset-request")
    public ResponseEntity<PasswordResetResponse> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request,
            HttpServletRequest httpRequest) {
        PasswordResetResponse response = resetService.initiatePasswordReset(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-confirm")
    public ResponseEntity<PasswordResetResponse> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirm request) {
        PasswordResetResponse response = resetService.confirmPasswordReset(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reset-token-valid/{token}")
    public ResponseEntity<Map<String, Boolean>> validateResetToken(
            @PathVariable String token) {
        boolean isValid = resetService.isTokenValid(token);
        return ResponseEntity.ok(Map.of("valid", isValid));
    }
}
