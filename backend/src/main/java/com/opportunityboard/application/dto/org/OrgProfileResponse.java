package com.opportunityboard.application.dto.org;

import com.opportunityboard.domain.enums.CompanySize;
import com.opportunityboard.domain.enums.OrgVerified;

import java.time.Instant;
import java.util.UUID;

public record OrgProfileResponse(
        UUID orgId,
        String orgName,
        String website,
        String description,
        String contactEmail,
        String contactPhone,
        String taxCode,
        String address,
        String industry,
        CompanySize companySize,
        String logoUrl,
        String logoAccessUrl,
        OrgVerified verifiedStatus,
        String verificationNote,
        Instant aiScannedAt,
        boolean needsUpdate,
        String updateHint
) {}
