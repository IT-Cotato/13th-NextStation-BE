package com.cotato.nextstation.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "프로필 수정 요청 (요청에 넘어온 필드만 부분 수정한다)")
public record MemberProfileUpdateRequest(

        @Schema(description = "닉네임. 한글/영문/숫자만 사용해 2~10자, 중복·금칙어 불가. 필드 자체를 생략하면 변경하지 않는다", example = "환승러")
        String nickname,

        @Schema(description = "프로필 이미지 URL. presigned URL로 업로드 완료 후 받은 imageUrl, 또는 카카오 로그인 응답으로 받은 profileImageUrl을 그대로 전달. 빈 문자열(\"\")을 보내면 프로필 이미지를 제거한다. 필드 자체를 생략하면 변경하지 않는다", example = "https://bucket.s3.ap-northeast-2.amazonaws.com/images/uploads/profile/1/uuid.jpg")
        @Size(max = 1000, message = "프로필 이미지 URL은 1000자를 초과할 수 없습니다.") // Member.profileImageUrl 컬럼 길이와 동일
        String profileImageUrl
) {
}