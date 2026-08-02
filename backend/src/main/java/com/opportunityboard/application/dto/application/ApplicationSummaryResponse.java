package com.opportunityboard.application.dto.application;

import com.opportunityboard.domain.enums.AppStatus;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** DTO trả về cho Student (my applications) và Provider (app list) — tránh lazy proxy serialize. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApplicationSummaryResponse {
    private UUID appId;
    private UUID oppId;
    private String title;      // opportunity title
    private String slug;       // opportunity slug (để link detail)
    private String orgName;
    private AppStatus status;
    private boolean isExternal;
    private String cvFile;
    private Instant appliedAt;
    private Instant decidedAt;
    private String studentName;   // chỉ dùng ở provider view
    private String studentEmail;
    private String coverLetter;
    private String providerNote;
    private String rejectionReason;
    private String aiModerationNote;
    private Instant aiScannedAt;
    private String screeningCriteria;
    private String major;
    private String university;
    private Short universityYear;
    private String skills;
}
