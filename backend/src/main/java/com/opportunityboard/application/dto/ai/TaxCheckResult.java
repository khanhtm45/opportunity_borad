package com.opportunityboard.application.dto.ai;

/**
 * Kiểm tra MST (thuế) — lớp pháp nhân org, tách khỏi AI quét tin đăng.
 */
public record TaxCheckResult(
        String taxCodeNormalized,
        boolean formatValid,
        boolean checksumValid,
        /** PASS | FAIL | MISSING */
        String status,
        String message
) {
    public static TaxCheckResult missing() {
        return new TaxCheckResult(null, false, false, "MISSING",
                "Chưa có MST — yêu cầu provider bổ sung mã số thuế tổ chức");
    }

    public boolean passed() {
        return "PASS".equals(status);
    }
}
