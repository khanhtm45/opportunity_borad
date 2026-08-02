package com.opportunityboard.application.dto.upload;

/** url = ref nội bộ ob-s3://… (lưu DB); viewUrl = link tạm để xem (không public bucket). */
public record UploadResponse(
        String url,
        String key,
        String viewUrl,
        String contentType,
        long sizeBytes,
        String purpose,
        boolean encryptedAtRest
) {}
