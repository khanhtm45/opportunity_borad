package com.opportunityboard.application.dto.opportunity;

import com.opportunityboard.application.dto.document.OppDocumentInput;
import com.opportunityboard.domain.enums.*;
import jakarta.validation.Valid;

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
        Long salaryMin,
        Long salaryMax,
        String salaryCurrency,
        Boolean salaryNegotiable,
        String selectionProcess,
        JobLevel jobLevel,
        ExperienceLevel experienceLevel,
        EducationLevel educationLevel,
        Integer headcount,
        EmploymentType employmentType,
        String addressDetail,
        String workingSchedule,
        String skills,
        LocationType location,
        WorkType workType,
        ApplyMode applyMode,
        String externalLink,
        String externalRef,
        String internalForm,
        String logoUrl,
        String bannerUrl,
        Instant deadline,
        List<UUID> domainIds,
        @Valid List<OppDocumentInput> documents
) {}
