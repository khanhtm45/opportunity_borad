package com.opportunityboard.application.dto.opportunity;

import com.opportunityboard.domain.enums.ApplyMode;
import com.opportunityboard.domain.enums.LocationType;
import com.opportunityboard.domain.enums.OppStatus;
import com.opportunityboard.domain.enums.WorkType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OpportunityDetailResponse(
        UUID oppId,
        UUID orgId,
        String title,
        String slug,
        String orgName,
        String logoUrl,
        String orgDescription,
        String categoryCode,
        String categoryName,
        String description,
        String requirements,
        String benefits,
        LocationType location,
        WorkType workType,
        ApplyMode applyMode,
        String externalLink,
        Instant deadline,
        OppStatus storedStatus,
        Instant publishedAt,
        boolean isFeatured,
        int viewCount,
        int bookmarkCount,
        int applicationCount,
        List<UUID> domainIds,
        List<OpportunityResponse> related
) {}
