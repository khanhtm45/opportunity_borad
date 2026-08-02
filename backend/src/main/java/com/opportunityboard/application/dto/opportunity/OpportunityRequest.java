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
        String salaryOrReward,
        String selectionProcess,
        LocationType location,
        WorkType workType,
        ApplyMode applyMode,
        String externalLink,
        String externalRef,
        String internalForm,
        String logoUrl,
        String bannerUrl,
        Instant deadline,
        List<UUID> domainIds
) {}
