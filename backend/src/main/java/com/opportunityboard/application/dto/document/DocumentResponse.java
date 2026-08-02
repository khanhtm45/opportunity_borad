package com.opportunityboard.application.dto.document;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID docId,
        String docType,
        String title,
        /** Ref lưu DB (ob-s3://… hoặc https://…) */
        String fileUrl,
        /** Link xem tạm (ký HMAC) — dùng cho &lt;a href&gt; / preview */
        String accessUrl,
        Instant createdAt
) {}
