package com.cotato.nextstation.global.security;

import com.cotato.nextstation.global.exception.CustomException;
import com.cotato.nextstation.global.exception.error.GlobalErrorCode;
import com.cotato.nextstation.global.jwt.AuthTokenClaims;
import com.cotato.nextstation.global.jwt.JwtProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

// @AuthenticationPrincipal이 붙은 컨트롤러 파라미터에 access token을 검증해 JwtPrincipal을 채워 넣는다
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtPrincipalArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                && JwtPrincipal.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                   NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String token = extractToken(webRequest.getHeader("Authorization"));

        Claims claims;
        try {
            claims = jwtProvider.parseClaims(token);
        } catch (ExpiredJwtException e) {
            log.warn("만료된 access token으로 요청");
            throw new CustomException(GlobalErrorCode.EXPIRED_TOKEN);
        } catch (JwtException e) {
            log.warn("유효하지 않은 access token으로 요청: {}", e.getMessage());
            throw new CustomException(GlobalErrorCode.INVALID_TOKEN);
        }

        if (!AuthTokenClaims.ACCESS_PURPOSE.equals(claims.get(AuthTokenClaims.PURPOSE_KEY, String.class))) {
            log.warn("purpose가 ACCESS가 아닌 토큰으로 요청");
            throw new CustomException(GlobalErrorCode.INVALID_TOKEN);
        }

        try {
            return new JwtPrincipal(Long.valueOf(claims.getSubject()));
        } catch (NumberFormatException e) {
            log.warn("subject가 memberId 형식이 아닌 access token: subject={}", claims.getSubject());
            throw new CustomException(GlobalErrorCode.INVALID_TOKEN);
        }
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            log.warn("Authorization 헤더 없이 인증이 필요한 API 요청");
            throw new CustomException(GlobalErrorCode.UNAUTHORIZED);
        }
        return authorizationHeader.substring(BEARER_PREFIX.length()).trim();
    }
}