package com.opportunityboard.application.dto.auth;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UUID userId,
        String role,
        String fullName
) {}
