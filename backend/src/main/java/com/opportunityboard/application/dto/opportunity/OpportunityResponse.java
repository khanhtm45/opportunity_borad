package com.opportunityboard.application.dto.opportunity;

import com.opportunityboard.domain.enums.*;

import java.time.Instant;
import java.util.UUID;

public record OpportunityResponse(
        UUID oppId,
        String title,
        String slug,
        String orgName,
        String logoUrl,
        String bannerUrl,
        String categoryCode,
        DisplayStatus displayStatus,     // OPEN / CLOSING_SOON / EXPIRED / HIDDEN
        Instant deadline,
        WorkType workType,
        LocationType location,
        boolean isFeatured,
        int viewCount,
        int bookmarkCount,
        int applicationCount,
        int shareCount
) {
    public enum DisplayStatus { OPEN, CLOSING_SOON, EXPIRED, HIDDEN }
}
