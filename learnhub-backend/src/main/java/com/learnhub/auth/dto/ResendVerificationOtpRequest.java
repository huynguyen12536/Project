package com.learnhub.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendVerificationOtpRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email
) {}
