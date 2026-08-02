package com.opportunityboard.application.dto.ai;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** AI quét CV / hồ sơ sinh viên theo tiêu chuẩn nhà đăng. */
public record ApplicationAiScanResponse(
        UUID appId,
        UUID oppId,
        String studentName,
        String studentEmail,
        String appStatus,
        String verdict,          // APPROVE | REVIEW | REJECT
        double confidence,
        String summary,
        List<String> strengths,
        List<String> gaps,
        List<String> risks,
        List<String> recommendations,
        String criteriaUsed,
        String model,
        Instant scannedAt,
        String appliedAction,    // NOTE_SAVED | NONE
        String nextAction,       // PROVIDER_REVIEW | REQUEST_UPDATE | ACCEPT | REJECT
        String moderationNote,
        String rawModelText
) {}
