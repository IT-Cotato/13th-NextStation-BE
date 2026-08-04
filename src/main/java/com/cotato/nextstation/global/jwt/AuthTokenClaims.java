package com.cotato.nextstation.global.jwt;

import java.time.Duration;

// 로그인 access/refresh 토큰 발급(AuthTokenService, KakaoLoginQueryService)과 검증(JwtPrincipalArgumentResolver)이 공유하는 claim/만료 상수
// purpose로 access/refresh를 구분해서, refresh token을 access token 자리에 잘못 흘려넣는 걸 막는다.
// REFRESH_TOKEN_EXPIRATION은 RefreshTokenCookieFactory의 쿠키 maxAge와도 값이 맞아야 하므로 반드시 여기서만 정의한다.
public final class AuthTokenClaims {

    public static final String PURPOSE_KEY = "purpose";
    public static final String ACCESS_PURPOSE = "ACCESS";
    public static final String REFRESH_PURPOSE = "REFRESH";

    // refreshToken 전용 claim: rotation + reuse detection에 쓰인다
    public static final String FAMILY_ID_KEY = "familyId";
    public static final String JTI_KEY = "jti";

    public static final Duration ACCESS_TOKEN_EXPIRATION = Duration.ofHours(1);
    public static final Duration REFRESH_TOKEN_EXPIRATION = Duration.ofDays(14);

    private AuthTokenClaims() {
    }
}