package com.opportunityboard.integration;

/**
 * Helper để gọi method throws checked exception bên trong lambda (biên dịch gọn).
 */
public final class AuthServiceTestHelper {
    @FunctionalInterface
    public interface ThrowingRunnable { void run() throws Exception; }

    public static void silent(ThrowingRunnable r) {
        try { r.run(); } catch (Exception e) { throw new RuntimeException(e); }
    }
}
