package com.cotato.nextstation.domain.auth.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// POST https://kauth.kakao.com/oauth/token 응답 매핑용 DTO
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoTokenResponse(

        // fetchUserInfo() 호출 한 번에만 쓰고 저장 안 함
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn,

        // 매 로그인마다 인가코드부터 새로 받는 구조라 이번 스코프에서는 사용 안 함
        @JsonProperty("refresh_token") String refreshToken,
        String scope
) {
}