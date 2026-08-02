package com.opportunityboard.application.dto.org;

import com.opportunityboard.domain.enums.CompanySize;
import jakarta.validation.constraints.Size;

public record OrgProfileUpdateRequest(
        @Size(max = 200) String orgName,
        String website,
        @Size(max = 2000) String description,
        String contactPhone,
        @Size(max = 40) String taxCode,
        @Size(max = 500) String address,
        @Size(max = 200) String industry,
        CompanySize companySize,
        @Size(max = 512) String logoUrl
) {}
