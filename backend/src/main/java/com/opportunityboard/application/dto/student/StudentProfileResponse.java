package com.opportunityboard.application.dto.student;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StudentProfileResponse(
        UUID profileId,
        String fullName,
        String email,
        String major,
        String university,
        Short universityYear,
        String cvUrl,
        boolean hasCv,
        List<String> skills,
        String bio,
        Instant updatedAt
) {}
