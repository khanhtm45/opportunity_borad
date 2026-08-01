package com.opportunityboard.application.dto.opportunity;

import com.opportunityboard.domain.enums.ApplyMode;
import com.opportunityboard.domain.enums.LocationType;
import com.opportunityboard.domain.enums.WorkType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OpportunityRequest(
        UUID categoryId,
        String title,
        String description,
        String requirements,
        String benefits,
        LocationType location,
        WorkType workType,
        ApplyMode applyMode,
        String externalLink,
        String internalForm,
        String logoUrl,
        Instant deadline,
        List<UUID> domainIds
) {}
