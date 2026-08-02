package com.opportunityboard.config;

import com.opportunityboard.infrastructure.ai.OpenRouterProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OpenRouterProperties.class)
public class AiConfig {
}
