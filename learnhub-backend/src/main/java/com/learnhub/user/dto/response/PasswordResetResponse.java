package com.learnhub.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetResponse {

    private Boolean success;
    private String message;
    private String resetLink;
    private LocalDateTime expiresAt;

    public static PasswordResetResponse initiated(String message) {
        return PasswordResetResponse.builder()
                .success(true)
                .message(message)
                .build();
    }

    public static PasswordResetResponse confirmed(String message) {
        return PasswordResetResponse.builder()
                .success(true)
                .message(message)
                .expiresAt(LocalDateTime.now())
                .build();
    }
}
