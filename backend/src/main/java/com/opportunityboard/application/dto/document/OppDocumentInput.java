package com.opportunityboard.application.dto.document;

import com.opportunityboard.domain.enums.OppDocType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OppDocumentInput(
        @NotNull OppDocType docType,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 512) String fileUrl
) {}
