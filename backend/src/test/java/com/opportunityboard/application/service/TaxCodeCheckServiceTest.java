package com.opportunityboard.application.service;

import com.opportunityboard.application.dto.ai.TaxCheckResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TaxCodeCheckServiceTest {

    private final TaxCodeCheckService svc = new TaxCodeCheckService();

    @Test
    void missingWhenBlank() {
        assertEquals("MISSING", svc.check(null).status());
        assertEquals("MISSING", svc.check("  ").status());
    }

    @Test
    void failWhenWrongLength() {
        assertEquals("FAIL", svc.check("123").status());
    }

    @Test
    void passWhenChecksumOk() {
        String valid = findValidMst();
        TaxCheckResult r = svc.check(valid);
        assertEquals("PASS", r.status());
        assertTrue(r.checksumValid());
    }

    @Test
    void failWhenChecksumBad() {
        String valid = findValidMst();
        char flip = valid.charAt(0) == '1' ? '2' : '1';
        String bad = flip + valid.substring(1);
        // may still pass checksum rarely — flip last digit of a valid one instead
        bad = valid.substring(0, 9) + (valid.charAt(9) == '0' ? '1' : '0');
        assertEquals("FAIL", svc.check(bad).status());
    }

    /** Sinh MST 10 số thỏa checksum để test ổn định. */
    private static String findValidMst() {
        for (int i = 0; i < 10_000_000; i++) {
            String nine = String.format("%09d", i);
            for (int d = 0; d <= 9; d++) {
                String cand = nine + d;
                if (TaxCodeCheckService.checksumValid(cand)) return cand;
            }
        }
        throw new IllegalStateException("no valid mst");
    }
}
