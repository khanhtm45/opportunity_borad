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
        DisplayStatus displayStatus,
        Instant deadline,
        WorkType workType,
        LocationType location,
        EmploymentType employmentType,
        JobLevel jobLevel,
        ExperienceLevel experienceLevel,
        Long salaryMin,
        Long salaryMax,
        String salaryCurrency,
        boolean salaryNegotiable,
        boolean isFeatured,
        int viewCount,
        int bookmarkCount,
        int applicationCount,
        int shareCount,
        OppStatus status,
        String rejectionReason,
        String aiModerationNote
) {
    public enum DisplayStatus { OPEN, CLOSING_SOON, EXPIRED, HIDDEN }
}
