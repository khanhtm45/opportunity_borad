package com.opportunityboard.application.dto.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApplicationScanRequest(
        @NotBlank @Size(max = 4000) String criteria
) {}
