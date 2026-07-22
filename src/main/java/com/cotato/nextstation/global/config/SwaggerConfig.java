package com.cotato.nextstation.global.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
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
// Authorization: Bearer {token} 헤더가 필요한 API용 인증 스킴
// 우측 상단 자물쇠(Authorize) 버튼에 토큰 값만 넣으면 Swagger UI가 모든 요청에 자동으로 헤더를 실어 보낸다.
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
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
                // 저장 탭 API는 /members/me 하위 경로를 쓰므로 함께 포함한다
                .pathsToMatch("/api/v1/courses/**", "/api/v1/members/me/**")
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

    // S3 이미지 업로드 관련 API
    @Bean
    public GroupedOpenApi imageApi() {
        return GroupedOpenApi.builder()
                .group("Image")
                .displayName("Image API")
                .packagesToScan("com.cotato.nextstation.domain.image.controller")
                .pathsToMatch("/api/v1/images/**")
                .build();
    }

    // 역 관련 API (역 검색 등)
    @Bean
    public GroupedOpenApi stationApi() {
        return GroupedOpenApi.builder()
                .group("Station")
                .displayName("Station API")
                .packagesToScan("com.cotato.nextstation.domain.station.controller")
                .pathsToMatch("/api/v1/stations/**")
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

    // 추천 관련 API (랜덤뽑기/맞춤추천)
    @Bean
    public GroupedOpenApi recommendationApi() {
        return GroupedOpenApi.builder()
                .group("Recommendation")
                .displayName("Recommendation API")
                .packagesToScan("com.cotato.nextstation.domain.recommendation.controller")
                .build();
    }

}
