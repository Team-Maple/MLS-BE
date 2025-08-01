package com.maple.api.common.presentation.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(servers = {@Server(url = "/", description = "Default Server URL")})
public class SwaggerConfig {

  @Bean
  public OpenAPI openAPI() {
    // SecuritySecheme명
    String jwtSchemeName = "Authorization";
    // API 요청헤더에 인증정보 포함
    SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);
    // SecuritySchemes 등록
    Components components = new Components()
      .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
        .name(jwtSchemeName)
        .type(SecurityScheme.Type.HTTP) // HTTP 방식
        .scheme("bearer")
        .bearerFormat("JWT")); // 토큰 형식을 지정하는 임의의 문자(Optional)

    return new OpenAPI()
      .info(apiInfo())
      .addSecurityItem(securityRequirement)
      .components(components);
  }

  private Info apiInfo() {
    return new Info()
      .title("Maple Land Api Specification")
      .description("Specification")
      .version("v1");
  }
}
