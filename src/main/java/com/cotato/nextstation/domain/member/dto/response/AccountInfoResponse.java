package com.cotato.nextstation.domain.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "계정 정보(가입한 이메일) 조회 응답")
public record AccountInfoResponse(

        @Schema(description = "가입 경로. 소셜 연동이 없으면 LOCAL", example = "KAKAO", allowableValues = {"LOCAL", "KAKAO", "APPLE"})
        String provider,

        @Schema(description = "가입한 이메일. 카카오 계정에 인증된 이메일이 없는 회원은 null", example = "user@example.com")
        String email
) {
}
