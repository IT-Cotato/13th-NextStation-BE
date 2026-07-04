package com.cotato.nextstation.global.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
        info = @Info(
                title = "NextStation API Docs",
                version = "v1.0.0",
                description = "NextStation Backend API Documentation",
                license = @License(name = "Apache 2.0", url = "http://www.apache.org/licenses/LICENSE-2.0")
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local Development"),
        }
)
@Configuration
public class SwaggerConfig {

    /**
     * User 관련 API
     */
    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("User")
                .displayName("User API")
                .packagesToScan("com.cotato.nextstation.domain.user.controller")
                .pathsToMatch("/api/users/**")
                .build();
    }

    /**
     * Auth 관련 API (인증/인가)
     */
    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("Auth")
                .displayName("Authentication API")
                .packagesToScan("com.cotato.nextstation.domain.auth.controller")
                .pathsToMatch("/api/auth/**")
                .build();
    }

}