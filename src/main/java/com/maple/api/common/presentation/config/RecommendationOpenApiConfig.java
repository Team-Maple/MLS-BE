package com.maple.api.common.presentation.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class RecommendationOpenApiConfig {

    private static final String AUTHORIZATION_SCHEME = "Authorization";

    @Bean
    public OpenApiCustomizer recommendationOptionalAuthentication() {
        return openApi -> {
            setOptionalAuthentication(openApi.getPaths().get("/api/v1/maps/recommendations"));
            setOptionalAuthentication(openApi.getPaths().get("/api/v2/maps/recommendations"));
        };
    }

    private void setOptionalAuthentication(PathItem pathItem) {
        if (pathItem == null) {
            return;
        }
        Operation operation = pathItem.getGet();
        if (operation == null) {
            return;
        }
        operation.setSecurity(List.of(
                new SecurityRequirement(),
                new SecurityRequirement().addList(AUTHORIZATION_SCHEME)
        ));
    }
}
