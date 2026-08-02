package com.opportunityboard.infrastructure.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.s3")
public record S3Properties(
        String enabled,
        String bucket,
        String region,
        String accessKey,
        String secretKey,
        String publicBaseUrl
) {
    public boolean isConfigured() {
        return "s3".equalsIgnoreCase(enabled != null ? enabled.trim() : "")
                && bucket != null && !bucket.isBlank()
                && region != null && !region.isBlank()
                && accessKey != null && !accessKey.isBlank()
                && secretKey != null && !secretKey.isBlank();
    }
}
