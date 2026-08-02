package com.opportunityboard.application.service;

import com.opportunityboard.application.dto.ai.TaxCheckResult;
import org.springframework.stereotype.Service;

/**
 * Kiểm tra định dạng + checksum MST Việt Nam (10 hoặc 13 số).
 * Không gọi API thuế bên ngoài — deterministic, dùng cho lớp verify org.
 */
@Service
public class TaxCodeCheckService {

    private static final int[] WEIGHTS = {31, 29, 23, 19, 17, 13, 7, 5, 3};

    public TaxCheckResult check(String rawTaxCode) {
        if (rawTaxCode == null || rawTaxCode.isBlank()) {
            return TaxCheckResult.missing();
        }
        String digits = rawTaxCode.replaceAll("[^0-9]", "");
        if (!(digits.length() == 10 || digits.length() == 13)) {
            return new TaxCheckResult(digits, false, false, "FAIL",
                    "MST phải gồm 10 số (doanh nghiệp) hoặc 13 số (chi nhánh)");
        }
        boolean checksumOk = checksumValid(digits.substring(0, 10));
        if (!checksumOk) {
            return new TaxCheckResult(digits, true, false, "FAIL",
                    "MST sai checksum — có thể gõ nhầm hoặc giả");
        }
        String msg = digits.length() == 13
                ? "MST chi nhánh hợp lệ (định dạng + checksum)"
                : "MST hợp lệ (định dạng + checksum)";
        return new TaxCheckResult(digits, true, true, "PASS", msg);
    }

    /** So khớp MST form với chuỗi đọc được từ giấy tờ (OCR/AI). */
    public boolean matchesDocumentText(String formTaxCode, String extractedHint) {
        TaxCheckResult form = check(formTaxCode);
        if (!form.passed() || extractedHint == null || extractedHint.isBlank()) return false;
        String hintDigits = extractedHint.replaceAll("[^0-9]", "");
        if (hintDigits.length() < 10) return false;
        String formDigits = form.taxCodeNormalized();
        return hintDigits.contains(formDigits) || formDigits.startsWith(hintDigits.substring(0, 10));
    }

    static boolean checksumValid(String tenDigits) {
        if (tenDigits == null || tenDigits.length() != 10 || !tenDigits.chars().allMatch(Character::isDigit)) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += (tenDigits.charAt(i) - '0') * WEIGHTS[i];
        }
        int check = 10 - (sum % 11);
        if (check == 10) check = 0;
        return check == (tenDigits.charAt(9) - '0');
    }
}
