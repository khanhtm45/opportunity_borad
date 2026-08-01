package com.opportunityboard.application.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProviderRegisterRequest(
        @NotBlank String orgName,
        String website,
        @Size(max = 2000) String description,
        @NotBlank @jakarta.validation.constraints.Email String contactEmail,
        @NotBlank String contactFullName,
        @Size(min = 8, max = 128) String password
) {}
