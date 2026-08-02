package com.opportunityboard.application.dto.auth;

import com.opportunityboard.application.dto.document.OrgDocumentInput;
import com.opportunityboard.domain.enums.CompanySize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProviderRegisterRequest(
        @NotBlank String orgName,
        String website,
        @Size(max = 2000) String description,
        @NotBlank @jakarta.validation.constraints.Email String contactEmail,
        String contactPhone,
        @Size(max = 40) String taxCode,
        @Size(max = 500) String address,
        @Size(max = 200) String industry,
        CompanySize companySize,
        @Size(max = 512) String logoUrl,
        @NotBlank String contactFullName,
        @Size(min = 8, max = 128) String password,
        @NotEmpty @Valid List<OrgDocumentInput> documents
) {}
