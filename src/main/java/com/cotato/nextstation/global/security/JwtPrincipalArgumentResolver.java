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
        String authorizationHeader = webRequest.getHeader("Authorization");

        // 자격 증명을 아예 보내지 않은 경우만 비로그인으로 본다.
        if (isBlank(authorizationHeader)) {
            if (!isRequired(parameter)) {
                return null;
            }
            log.warn("Authorization 헤더 없이 인증이 필요한 API 요청");
            throw new CustomException(GlobalErrorCode.UNAUTHORIZED);
        }

        // 여기서부터는 required와 무관하게 401이다. 헤더를 보냈다는 건 로그인한 줄 알고 있다는 뜻이라,
        // 조용히 비로그인으로 넘기면 사용자는 좋아요 표시만 빠진 화면을 보고 원인을 알 수 없다.
        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            log.warn("Bearer 형식이 아닌 Authorization 헤더로 요청");
            throw new CustomException(GlobalErrorCode.INVALID_TOKEN);
        }

        String token = extractToken(authorizationHeader);
        if (token.isEmpty()) {
            log.warn("빈 Bearer 토큰으로 요청");
            throw new CustomException(GlobalErrorCode.INVALID_TOKEN);
        }

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

    // 값이 빈 헤더는 보내지 않은 것과 같게 취급한다.
    // 로그아웃 상태에서 빈 Authorization 헤더를 실어 보내는 클라이언트가 있어도 비로그인으로 동작해야 한다.
    private boolean isBlank(String authorizationHeader) {
        return authorizationHeader == null || authorizationHeader.isBlank();
    }

    private String extractToken(String authorizationHeader) {
        return authorizationHeader.substring(BEARER_PREFIX.length()).trim();
    }

    private boolean isRequired(MethodParameter parameter) {
        AuthenticationPrincipal annotation = parameter.getParameterAnnotation(AuthenticationPrincipal.class);
        return annotation == null || annotation.required();
    }
}