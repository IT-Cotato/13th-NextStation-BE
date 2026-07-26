package com.cotato.nextstation.domain.auth.util;

import com.cotato.nextstation.domain.auth.service.query.LoginQueryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

// 로그인/카카오로그인 등 refreshToken을 쿠키로 내려주는 모든 컨트롤러가 공유하는 쿠키 생성기
@Component
public class RefreshTokenCookieFactory {

    public static final String COOKIE_NAME = "refreshToken";

    private final boolean secure;
    private final String sameSite;

    public RefreshTokenCookieFactory(@Value("${auth.refresh-cookie.secure}") boolean secure,
                                      @Value("${auth.refresh-cookie.same-site}") String sameSite) {
        this.secure = secure;
        this.sameSite = sameSite;
    }

    public ResponseCookie create(String refreshToken) {
        return ResponseCookie.from(COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(LoginQueryService.REFRESH_TOKEN_EXPIRATION)
                .build();
    }
}