package com.cotato.nextstation.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원가입 비밀번호 설정 응답")
public record SignupResponse(

        @Schema(description = "생성된 회원 id", example = "1")
        Long memberId,

        @Schema(description = "프로필 설정 API 호출 시 Authorization: Bearer 헤더에 담아 보내는 가입 전용 토큰. access token이 아니며 30분 후 만료된다.", example = "eyJhbGciOiJIUzI1NiJ9...")
        String signupToken
) {
}