package com.opportunityboard.application.dto.ai;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * AI quét hồ sơ tin đăng (chương trình/ủy quyền) — tách khỏi check thuế org.
 */
public record OpportunityDocScanResponse(
        UUID oppId,
        String title,
        String orgName,
        String orgVerifiedStatus,
        String verdict,              // APPROVE | REVIEW | REJECT
        double confidence,
        String summary,
        List<String> findings,
        List<String> risks,
        List<String> recommendations,
        List<String> contentMismatches,
        int documentCount,
        String model,
        Instant scannedAt,
        String previousOppStatus,
        /** NOTE_SAVED | REJECTED | NONE */
        String appliedAction,
        /** DONE | ADMIN_REVIEW | REQUEST_UPDATE | REJECT_OPP */
        String nextAction,
        /** Lý do đầy đủ để Admin gửi yêu cầu cập nhật / từ chối */
        String moderationNote,
        String rawModelText
) {}
