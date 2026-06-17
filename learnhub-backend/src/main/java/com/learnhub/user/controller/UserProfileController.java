package com.learnhub.user.controller;

import com.learnhub.common.util.AuthenticationUtil;
import com.learnhub.user.dto.request.UpdateProfileRequest;
import com.learnhub.user.dto.response.UserProfileResponse;
import com.learnhub.user.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {

    private final UserProfileService profileService;
    private final AuthenticationUtil authenticationUtil;

    @GetMapping("/profile")
    @PreAuthorize("hasRole('LEARNER')")
    public ResponseEntity<UserProfileResponse> getProfile() {
        UUID userId = authenticationUtil.getCurrentUserId();
        UserProfileResponse profile = profileService.getProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('LEARNER')")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = authenticationUtil.getCurrentUserId();
        UserProfileResponse updated = profileService.updateProfile(userId, request);
        return ResponseEntity.ok(updated);
    }
}
