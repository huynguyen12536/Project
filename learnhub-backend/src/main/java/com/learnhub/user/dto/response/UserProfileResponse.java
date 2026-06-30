package com.learnhub.user.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.learnhub.user.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * DTO for user profile response.
 *
 * Matches API contract specification in user-profile-api-contract.md.
 * Returns all publicly accessible user profile information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {

    private String id;

    private String username;

    private String email;

    private String firstName;

    private String lastName;

    private String bio;

    private String avatarUrl;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private Instant createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private Instant updatedAt;

    /**
     * Convert User entity to UserProfileResponse DTO.
     *
     * @param user the user entity
     * @return the user profile response DTO
     */
    public static UserProfileResponse from(User user) {
        return UserProfileResponse.builder()
            .id(user.getId().toString())
            .username(user.getUsername())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .bio(user.getBio())
            .avatarUrl(user.getAvatarUrl())
            .createdAt(convertToInstant(user.getCreatedAt()))
            .updatedAt(convertToInstant(user.getUpdatedAt()))
            .build();
    }

    public static UserProfileResponse fromUser(User user) {
        return from(user);
    }

    /**
     * Convert LocalDateTime to Instant (UTC).
     */
    private static Instant convertToInstant(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(ZoneId.of("UTC")).toInstant();
    }
}
