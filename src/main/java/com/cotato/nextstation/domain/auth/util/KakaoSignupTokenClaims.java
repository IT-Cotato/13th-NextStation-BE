package com.cotato.nextstation.domain.auth.util;

// kakaoSignupToken 발급(KakaoLoginQueryService)과 검증(KakaoSignupCommandService)이 공유하는 claim 상수
// 주의: subject는 memberId가 아니라 카카오 회원번호(providerUserId)다 -> 다른 토큰과 다름
public final class KakaoSignupTokenClaims {

    public static final String PURPOSE_KEY = "purpose";
    public static final String KAKAO_SIGNUP_PURPOSE = "KAKAO_SIGNUP";

    public static final String EMAIL_KEY = "email";
    public static final String NICKNAME_KEY = "kakaoNickname";
    public static final String PROFILE_IMAGE_URL_KEY = "kakaoProfileImageUrl";

    private KakaoSignupTokenClaims() {
    }
}