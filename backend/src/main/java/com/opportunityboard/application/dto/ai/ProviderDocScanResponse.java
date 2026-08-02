package com.opportunityboard.application.dto.ai;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Kết quả AI quét hồ sơ TỔ CHỨC (thuế/pháp nhân) — tách khỏi quét tin đăng. */
public record ProviderDocScanResponse(
        UUID orgId,
        String orgName,
        String verdict,              // APPROVE | REVIEW | REJECT
        double confidence,
        String summary,
        List<String> findings,
        List<String> risks,
        List<String> recommendations,
        int documentCount,
        String model,
        Instant scannedAt,
        String previousVerifiedStatus,
        /** VERIFIED | NEEDS_UPDATE | NONE */
        String appliedAction,
        /** DONE | PROVIDER_UPDATE | MANUAL_REVIEW */
        String nextAction,
        String verificationNote,
        /** LOW | MEDIUM | HIGH — rủi ro giả mạo / AI-gen */
        String forgeryRisk,
        boolean aiGeneratedSuspected,
        List<String> authenticitySignals,
        List<String> consistencyIssues,
        /** Check MST deterministic — lớp thuế */
        TaxCheckResult taxCheck,
        String rawModelText
) {}
