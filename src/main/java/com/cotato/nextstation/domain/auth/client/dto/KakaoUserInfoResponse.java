package com.cotato.nextstation.domain.auth.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// GET https://kapi.kakao.com/v2/user/me 응답 매핑용 DTO
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoUserInfoResponse(

        // 앱별 고유/영구 고정값 -> MemberSocialAccount.providerUserId로 저장
        Long id,

        @JsonProperty("kakao_account")
        KakaoAccount kakaoAccount
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KakaoAccount(
            String email,

            // 이메일 동의 안 하면 카카오가 null로 내려줄 수 있어 Boolean(박싱)으로 받음 - boolean이면 파싱 불가
            @JsonProperty("is_email_valid")
            Boolean isEmailValid,

            // email이 있어도 이게 true일 때만 신뢰(비즈 앱 미전환 시 미인증 이메일일 수 있음)
            @JsonProperty("is_email_verified")
            Boolean isEmailVerified,

            Profile profile
    ) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Profile(
                String nickname,

                @JsonProperty("profile_image_url")
                String profileImageUrl
        ) {
        }
    }

    // kakao_account/profile 전체가 null일 수 있음(선택 동의 거부) -> 항상 이 메서드로 접근
    public String extractVerifiedEmail() {
        // isEmailVerified()가 Boolean(nullable)이라 !로 언박싱하면 null일 때 NPE - Boolean.TRUE.equals로 안전하게 비교
        if (kakaoAccount == null || kakaoAccount.email() == null || !Boolean.TRUE.equals(kakaoAccount.isEmailVerified())) {
            return null;
        }
        return kakaoAccount.email();
    }

    public String extractNickname() {
        if (kakaoAccount == null || kakaoAccount.profile() == null) {
            return null;
        }
        return kakaoAccount.profile().nickname();
    }

    public String extractProfileImageUrl() {
        if (kakaoAccount == null || kakaoAccount.profile() == null) {
            return null;
        }
        return kakaoAccount.profile().profileImageUrl();
    }
}