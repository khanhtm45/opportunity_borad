package com.opportunityboard.application.dto.student;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

public record StudentProfileUpdateRequest(
        @Size(max = 120) String major,
        @Size(max = 150) String university,
        @Min(1) @Max(8) Short universityYear,
        @Size(max = 512) String cvUrl,
        List<@Size(max = 60) String> skills,
        @Size(max = 2000) String bio
) {}
