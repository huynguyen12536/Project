package com.learnhub.user.controller;

import com.learnhub.user.dto.request.UserProfileUpdateRequest;
import com.learnhub.user.dto.request.UserProfilePatchRequest;
import com.learnhub.user.dto.response.AvatarUploadResponse;
import com.learnhub.user.dto.response.ErrorResponse;
import com.learnhub.user.dto.response.UserProfileResponse;
import com.learnhub.user.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * REST controller for user profile management.
 *
 * Exposes endpoints for retrieving, updating, and managing user profiles.
 * Includes avatar upload handling and authorization checks.
 *
 * API Contract: docs/user-profile-api-contract.md
 */
@RestController
@RequestMapping("/api/v1/users")
@Slf4j
public class UserProfileController {

    private final UserService userService;

    public UserProfileController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /api/v1/users/{userId}
     * Retrieve user profile by ID.
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<UserProfileResponse> getUserProfile(
            @PathVariable UUID userId) {
        log.info("GET /api/v1/users/{} - Fetch user profile", userId);

        UserProfileResponse profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    /**
     * PUT /api/v1/users/{userId}
     * Update user profile (all fields except avatar).
     */
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<UserProfileResponse> updateUserProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody UserProfileUpdateRequest updateRequest) {
        log.info("PUT /api/v1/users/{} - Update user profile", userId);

        UserProfileResponse updatedProfile = userService.updateUserProfile(userId, updateRequest);
        return ResponseEntity.ok(updatedProfile);
    }

    /**
     * PATCH /api/v1/users/{userId}
     * Partially update a user profile.
     */
    @PatchMapping("/{userId}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<UserProfileResponse> patchUserProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody UserProfilePatchRequest patchRequest) {
        log.info("PATCH /api/v1/users/{} - Patch user profile", userId);

        UserProfileResponse updatedProfile = userService.patchUserProfile(userId, patchRequest);
        return ResponseEntity.ok(updatedProfile);
    }

    /**
     * POST /api/v1/users/{userId}/avatar
     * Upload user avatar (multipart file).
     */
    @PostMapping("/{userId}/avatar")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<AvatarUploadResponse> uploadAvatar(
            @PathVariable UUID userId,
            @RequestParam("file") MultipartFile file) {
        log.info("POST /api/v1/users/{}/avatar - Upload avatar", userId);

        try {
            AvatarUploadResponse response = userService.uploadAvatar(userId, file);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Failed to upload avatar for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Avatar upload failed: " + e.getMessage());
        }
    }

    /**
     * GET /api/v1/users
     * Retrieve all users with pagination (admin only).
     */
    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Page<UserProfileResponse>> getAllUsers(Pageable pageable) {
        log.info("GET /api/v1/users - Fetch all users (paginated)");

        Page<UserProfileResponse> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    /**
     * DELETE /api/v1/users/{userId}
     * Delete user account (admin or self only).
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        log.info("DELETE /api/v1/users/{} - Delete user account", userId);

        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Exception handler for IllegalArgumentException.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Validation error: {}", e.getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .error(e.getMessage())
            .code("VALIDATION_ERROR")
            .build();

        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Exception handler for file upload errors.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        log.error("Runtime error: {}", e.getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .error(e.getMessage())
            .code("INTERNAL_ERROR")
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
