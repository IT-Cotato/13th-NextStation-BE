package com.cotato.nextstation.domain.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "다른 회원 프로필 조회 응답")
public record OtherMemberProfileResponse(

        @Schema(description = "회원 id", example = "2")
        Long memberId,

        @Schema(description = "닉네임", example = "환승러")
        String nickname,

        @Schema(description = "프로필 이미지 URL. 설정하지 않았으면 null", example = "https://bucket.s3.region.amazonaws.com/images/uploads/profile//2.png")
        String profileImageUrl,

        @Schema(description = "모은 스탬프(방문한 역) 개수. 같은 역에서 여러 코스를 완료해도 1개로 센다.", example = "12")
        long stampCount,

        @Schema(description = "공개한 코스 개수. 여행일지가 공개된 코스만 센다.", example = "5")
        long publicCourseCount
) {
}
