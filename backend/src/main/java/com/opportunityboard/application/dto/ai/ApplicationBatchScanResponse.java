package com.opportunityboard.application.dto.ai;

import java.util.List;
import java.util.UUID;

/** Kết quả quét hàng loạt — nhóm theo verdict để nhà đăng xem. */
public record ApplicationBatchScanResponse(
        UUID oppId,
        String oppTitle,
        String criteria,
        int scannedCount,
        List<ApplicationAiScanResponse> results,
        List<ApplicationAiScanResponse> approveGroup,
        List<ApplicationAiScanResponse> reviewGroup,
        List<ApplicationAiScanResponse> rejectGroup
) {}
