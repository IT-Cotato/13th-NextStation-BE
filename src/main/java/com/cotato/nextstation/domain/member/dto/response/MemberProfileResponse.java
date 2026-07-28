package com.cotato.nextstation.domain.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 프로필 조회 응답")
public record MemberProfileResponse(

        @Schema(description = "회원 id", example = "1")
        Long memberId,

        @Schema(description = "닉네임", example = "환승러")
        String nickname,

        @Schema(description = "프로필 이미지 URL. 설정하지 않았으면 null", example = "https://bucket.s3.region.amazonaws.com/images/uploads/profile//1.png")
        String profileImageUrl
) {
}