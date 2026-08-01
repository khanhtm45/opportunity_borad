package com.opportunityboard.application.dto.auth;

import com.opportunityboard.domain.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotNull UserRole role,
        @NotBlank @Email String email,
        @Size(min = 8, max = 128) String password,
        @NotBlank String fullName
) {}
