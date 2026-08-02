package com.opportunityboard.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${springdoc.server-url:}")
    private String serverUrl;

    @Bean
    public OpenAPI opportunityBoardOpenApi() {
        final String scheme = "bearerAuth";
        OpenAPI api = new OpenAPI()
                .info(new Info()
                        .title("Opportunity Board API")
                        .description("REST API `/api/v1` — Student / Provider / Admin. "
                                + "Authorize bằng JWT (login → accessToken).")
                        .version("v1")
                        .contact(new Contact()
                                .name("Opportunity Board")
                                .url("https://github.com/khanhtm45/opportunity_borad")))
                .components(new Components().addSecuritySchemes(scheme,
                        new SecurityScheme()
                                .name(scheme)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Dán accessToken từ POST /api/v1/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(scheme));

        if (serverUrl != null && !serverUrl.isBlank()) {
            api.servers(List.of(new Server().url(serverUrl.trim()).description("API base")));
        }
        return api;
    }
}
