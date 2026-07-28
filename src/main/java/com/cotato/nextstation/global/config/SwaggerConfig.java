package com.cotato.nextstation.global.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
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
// signupTokenAuth: 회원가입 비밀번호 설정(/signup) 응답의 signupToken, 프로필 설정(/profile) API 전용
@SecurityScheme(
        name = "signupTokenAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "회원가입 비밀번호 설정(/signup) 응답의 signupToken. 프로필 설정(/profile) API 호출 시에만 사용한다."
)
// accessTokenAuth: 로그인(/login) 응답의 access token, 로그인 유지가 필요한 API 전반에서 사용
@SecurityScheme(
        name = "accessTokenAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "로그인(/login) 응답의 access token. 발급 후 1시간 만료되며, 로그인이 필요한 API 호출 시 사용한다."
)
// refreshTokenAuth: 로그인(/login) 시 httpOnly 쿠키로 내려가는 refresh token
@SecurityScheme(
        name = "refreshTokenAuth",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.COOKIE,
        paramName = "refreshToken",
        description = "로그인(/login) 시 httpOnly 쿠키로 내려가는 refresh token. Authorize 입력창에 로그인 응답 쿠키의 refreshToken 값만(접두사 없이) 넣으면 된다."
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
                // 저장 탭은 /members/me, 장소 상세의 코스 목록은 /places 하위 경로를 쓰므로 함께 포함한다.
                // 패키지로도 걸러지므로 다른 도메인의 같은 경로 API가 섞이지는 않는다.
                .pathsToMatch("/api/v1/courses/**", "/api/v1/members/me/**", "/api/v1/places/*/courses")
                .build();
    }

    // 회원 관련 API (내 프로필 등)
    @Bean
    public GroupedOpenApi memberApi() {
        return GroupedOpenApi.builder()
                .group("Member")
                .displayName("Member API")
                .packagesToScan("com.cotato.nextstation.domain.member.controller")
                .pathsToMatch("/api/v1/members/**")
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
                .pathsToMatch("/api/v1/stamps/**", "/api/v1/courses/*/complete")
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
