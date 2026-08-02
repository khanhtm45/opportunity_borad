package com.opportunityboard.infrastructure.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.openrouter")
public record OpenRouterProperties(
        String apiKey,
        String baseUrl,
        String model,
        String siteUrl,
        String siteName,
        int timeoutSeconds
) {
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
