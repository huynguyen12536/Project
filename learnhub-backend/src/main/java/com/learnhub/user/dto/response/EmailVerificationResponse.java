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
public class EmailVerificationResponse {

    private Boolean emailVerified;
    private LocalDateTime emailVerifiedAt;
    private String message;
    private Long resendCountdown;

    public static EmailVerificationResponse success() {
        return EmailVerificationResponse.builder()
                .emailVerified(true)
                .emailVerifiedAt(LocalDateTime.now())
                .message("Email verified successfully")
                .build();
    }
}
