package com.opportunityboard.application.dto.application;

import com.opportunityboard.domain.enums.AppStatus;

public record AppStatusUpdateRequest(AppStatus status, String note) {}
