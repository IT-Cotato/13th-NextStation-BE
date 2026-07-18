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
                @Server(url = "https://3.37.77.188.nip.io", description = "Production Server"),
        }
)
@Configuration
public class SwaggerConfig {


    // Auth 관련 API (인증/인가)
    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("Auth")
                .displayName("Authentication API")
                .packagesToScan("com.cotato.nextstation.domain.auth.controller")
                .pathsToMatch("/api/v1/auth/**")
                .build();
    }

    // Place 관련 API (장소)
    @Bean
    public GroupedOpenApi placeApi() {
        return GroupedOpenApi.builder()
                .group("Place")
                .displayName("Place API")
                .packagesToScan("com.cotato.nextstation.domain.place.controller")
                .pathsToMatch("/api/v1/places/**")
                .build();
    }

    // 코스 관련 API
    @Bean
    public GroupedOpenApi courseApi() {
        return GroupedOpenApi.builder()
                .group("Course")
                .displayName("Course API")
                .packagesToScan("com.cotato.nextstation.domain.course.controller")
                .pathsToMatch("/api/v1/courses/**")
                .build();
    }

    // 출발역 즐겨찾기 관련 API
    @Bean
    public GroupedOpenApi departureStationApi() {
        return GroupedOpenApi.builder()
                .group("DepartureStation")
                .displayName("Departure Station API")
                .packagesToScan("com.cotato.nextstation.domain.departure.controller")
                .pathsToMatch("/api/v1/departure-stations/**")
                .build();
    }

    // Stamp 관련 API
    @Bean
    public GroupedOpenApi stampApi() {
        return GroupedOpenApi.builder()
                .group("Stamp")
                .displayName("Stamp API")
                .packagesToScan("com.cotato.nextstation.domain.stamp.controller")
                .pathsToMatch("/api/v1/stamps/**")
                .build();
    }

}