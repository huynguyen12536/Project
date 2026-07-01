package com.learnhub.user.service;

import com.learnhub.file.FileStorageService;
import com.learnhub.common.util.AuthenticationUtil;
import com.learnhub.user.dto.request.UserProfilePatchRequest;
import com.learnhub.user.dto.request.UserProfileUpdateRequest;
import com.learnhub.user.dto.response.AvatarUploadResponse;
import com.learnhub.user.dto.response.UserProfileResponse;
import com.learnhub.user.model.User;
import com.learnhub.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/**
 * Service layer for user profile management.
 *
 * Handles CRUD operations, profile updates, and avatar management.
 * Includes validation, error handling, and transaction management.
 */
@Service
@Transactional
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final AuthenticationUtil authenticationUtil;

    public UserService(
        UserRepository userRepository,
        FileStorageService fileStorageService,
        AuthenticationUtil authenticationUtil
    ) {
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.authenticationUtil = authenticationUtil;
    }

    /**
     * Retrieve user profile by ID.
     *
     * @param userId the user ID
     * @return user profile response
     * @throws IllegalArgumentException if user not found
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(UUID userId) {
        log.debug("Fetching user profile: {}", userId);
        ensureProfileAccess(userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        return UserProfileResponse.fromUser(user);
    }

    /**
     * Get all users with pagination.
     *
     * @param pageable pagination parameters
     * @return paginated user profiles
     */
    @Transactional(readOnly = true)
    public Page<UserProfileResponse> getAllUsers(Pageable pageable) {
        log.debug("Fetching all users with pagination: {}", pageable);

        return userRepository.findAll(pageable)
            .map(UserProfileResponse::fromUser);
    }

    /**
     * Update user profile (all fields except avatar).
     *
     * @param userId the user ID
     * @param updateRequest the update request
     * @return updated user profile response
     * @throws IllegalArgumentException if user not found or email duplicate
     */
    public UserProfileResponse updateUserProfile(UUID userId, UserProfileUpdateRequest updateRequest) {
        log.debug("Updating user profile: {}", userId);
        ensureProfileAccess(userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Check if email is being changed and if it's already taken
        if (!user.getEmail().equals(updateRequest.getEmail())) {
            if (userRepository.existsByEmail(updateRequest.getEmail())) {
                log.warn("Email already in use: {}", updateRequest.getEmail());
                throw new IllegalArgumentException("Email already in use: " + updateRequest.getEmail());
            }
        }

        // Update user fields
        user.setFirstName(updateRequest.getFirstName());
        user.setLastName(updateRequest.getLastName());
        user.setEmail(updateRequest.getEmail());
        user.setBio(updateRequest.getBio());
        user.setPhone(updateRequest.getPhone());
        user.setLocation(updateRequest.getLocation());

        User updatedUser = userRepository.save(user);
        log.info("User profile updated: {}", userId);

        return UserProfileResponse.fromUser(updatedUser);
    }

    public UserProfileResponse patchUserProfile(UUID userId, UserProfilePatchRequest patchRequest) {
        log.debug("Patching user profile: {}", userId);
        ensureProfileAccess(userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (patchRequest.getEmail() != null) {
            String nextEmail = patchRequest.getEmail().trim();
            if (nextEmail.isEmpty()) {
                throw new IllegalArgumentException("Email is required");
            }
            if (!user.getEmail().equals(nextEmail) && userRepository.existsByEmail(nextEmail)) {
                log.warn("Email already in use: {}", nextEmail);
                throw new IllegalArgumentException("Email already in use: " + nextEmail);
            }
            user.setEmail(nextEmail);
        }

        if (patchRequest.getFirstName() != null) {
            String nextFirstName = patchRequest.getFirstName().trim();
            if (nextFirstName.isEmpty()) {
                throw new IllegalArgumentException("First name is required");
            }
            user.setFirstName(nextFirstName);
        }

        if (patchRequest.getLastName() != null) {
            String nextLastName = patchRequest.getLastName().trim();
            if (nextLastName.isEmpty()) {
                throw new IllegalArgumentException("Last name is required");
            }
            user.setLastName(nextLastName);
        }

        if (patchRequest.getBio() != null) {
            user.setBio(patchRequest.getBio().trim());
        }

        if (patchRequest.getPhone() != null) {
            user.setPhone(patchRequest.getPhone().trim());
        }

        if (patchRequest.getLocation() != null) {
            user.setLocation(patchRequest.getLocation().trim());
        }

        User updatedUser = userRepository.save(user);
        log.info("User profile patched: {}", userId);

        return UserProfileResponse.fromUser(updatedUser);
    }

    /**
     * Upload and update user avatar.
     *
     * @param userId the user ID
     * @param file the avatar file
     * @return avatar upload response with new URL
     * @throws IllegalArgumentException if user not found or file invalid
     * @throws IOException if file upload fails
     */
    public AvatarUploadResponse uploadAvatar(UUID userId, MultipartFile file) throws IOException {
        log.debug("Uploading avatar for user: {}", userId);
        ensureProfileAccess(userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        String previousAvatarUrl = user.getAvatarUrl();
        String newAvatarUrl = fileStorageService.uploadFile(file, userId);
        user.setAvatarUrl(newAvatarUrl);

        User updatedUser = userRepository.save(user);
        if (previousAvatarUrl != null
            && !previousAvatarUrl.isBlank()
            && !isSameStorageObject(previousAvatarUrl, newAvatarUrl)
            && fileStorageService.fileExists(previousAvatarUrl)) {
            try {
                fileStorageService.deleteFile(previousAvatarUrl);
            } catch (IOException e) {
                log.warn("Failed to delete previous avatar for user {}: {}", userId, e.getMessage());
            }
        }

        log.info("Avatar uploaded successfully for user: {}", userId);

        return AvatarUploadResponse.builder()
            .message(previousAvatarUrl == null || previousAvatarUrl.isBlank()
                ? "Avatar uploaded successfully"
                : "Avatar updated successfully")
            .avatarUrl(newAvatarUrl)
            .fileSize(file.getSize())
            .uploadedAt(Instant.now())
            .build();
    }

    /**
     * Delete a user (account deletion).
     *
     * @param userId the user ID to delete
     * @throws IllegalArgumentException if user not found
     */
    public void deleteUser(UUID userId) {
        log.debug("Deleting user account: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Delete avatar from storage
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            try {
                fileStorageService.deleteFile(user.getAvatarUrl());
                log.debug("Deleted avatar for user: {}", userId);
            } catch (IOException e) {
                log.warn("Failed to delete avatar for user {}: {}", userId, e.getMessage());
            }
        }

        // Delete user from database
        userRepository.deleteById(userId);
        log.info("User account deleted: {}", userId);
    }

    private void ensureProfileAccess(UUID userId) {
        if (isCurrentUserAdmin()) {
            return;
        }

        UUID currentUserId = authenticationUtil.getCurrentUserId();
        if (!userId.equals(currentUserId)) {
            log.warn("Forbidden profile access. currentUserId={}, targetUserId={}", currentUserId, userId);
            throw new AccessDeniedException("You do not have permission to access this profile");
        }
    }

    private boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private boolean isSameStorageObject(String firstUrl, String secondUrl) {
        return normalizeStoragePath(firstUrl).equals(normalizeStoragePath(secondUrl));
    }

    private String normalizeStoragePath(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return "";
        }

        try {
            return URI.create(fileUrl).getPath();
        } catch (Exception exception) {
            return fileUrl;
        }
    }
}
