package com.cotato.nextstation.domain.image.dto.request;

import com.cotato.nextstation.domain.image.enums.S3Folder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

// TODO: access token 인증 미들웨어가 생기면 memberId는 요청 body 대신 인증 컨텍스트에서 꺼내도록 변경
@Schema(description = "다중 이미지 업로드용 presigned URL 발급 요청")
public record PresignedUrlsRequest(
        @NotNull(message = "업로드 대상 폴더는 필수입니다.")
        S3Folder folder,

        @Schema(description = "업로드 주체의 회원 id, 임시로 body에서 받는다(위 TODO 참고)", example = "1")
        @NotNull(message = "memberId는 필수입니다.")
        Long memberId,

        @Schema(description = "여행일지 id", example = "10")
        Long journalId,

        @Schema(description = "원본 파일명(확장자 포함)", example = "profile.jpg")
        @NotBlank(message = "파일명은 필수입니다.")
        List<String> fileNames
) {
}


