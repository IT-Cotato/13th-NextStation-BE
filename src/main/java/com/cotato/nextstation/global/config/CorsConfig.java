package com.cotato.nextstation.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private static final String[] ALLOWED_METHODS = {"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"};
    private static final long PREFLIGHT_CACHE_SECONDS = 3600;

    private final List<String> allowedOrigins;

    public CorsConfig(@Value("${app.cors.allowed-origins:}") String allowedOriginsProperty) {
        this.allowedOrigins = Arrays.stream(allowedOriginsProperty.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
    }

    // 로그인 응답의 refreshToken 쿠키를 프론트가 주고받으려면 CORS도 자격 증명(쿠키) 허용 필요
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (allowedOrigins.isEmpty()) {
            return;
        }

        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods(ALLOWED_METHODS)
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(PREFLIGHT_CACHE_SECONDS);
    }
}