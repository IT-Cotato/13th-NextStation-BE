package com.cotato.nextstation.domain.auth.dto.response;

import com.cotato.nextstation.domain.member.entity.MemberStatus;
import io.swagger.v3.oas.annotations.media.Schema;

// TODO: access token 발급 로직이 생기면 accessToken 필드 추가, refresh token은 쿠키로 내려줄 것
@Schema(description = "프로필 설정 응답")
public record ProfileSetupResponse(

        @Schema(description = "회원 id", example = "1")
        Long memberId,

        @Schema(description = "설정된 닉네임", example = "환승러")
        String nickname,

        @Schema(description = "프로필 설정 완료 후 회원 상태", example = "ACTIVE")
        MemberStatus status
) {
}