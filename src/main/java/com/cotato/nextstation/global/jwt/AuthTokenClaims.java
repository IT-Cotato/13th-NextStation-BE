package com.cotato.nextstation.global.jwt;

// 로그인 access/refresh 토큰 발급(LoginQueryService)과 검증(JwtPrincipalArgumentResolver)이 공유하는 claim 상수
// purpose로 access/refresh를 구분해서, refresh token을 access token 자리에 잘못 흘려넣는 걸 막는다.
public final class AuthTokenClaims {

    public static final String PURPOSE_KEY = "purpose";
    public static final String ACCESS_PURPOSE = "ACCESS";
    public static final String REFRESH_PURPOSE = "REFRESH";

    private AuthTokenClaims() {
    }
}