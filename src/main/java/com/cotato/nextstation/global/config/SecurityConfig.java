package com.cotato.nextstation.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

// 인증/인가는 JwtPrincipalArgumentResolver(컨트롤러 파라미터에 @AuthenticationPrincipal을 붙이는 옵트인 방식)가 담당한다.
// 이 설정은 spring-boot-starter-security를 추가하면 자동으로 걸리는 기본 잠금(HTTP Basic 로그인, 랜덤 생성 비밀번호, CSRF, 세션 기반 로그인폼)만 해제해서 기존 동작을 그대로 유지하는 최소 구성이다.
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}