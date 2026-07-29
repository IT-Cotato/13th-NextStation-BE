package com.cotato.nextstation.domain.image.dto.request;

import com.cotato.nextstation.domain.image.enums.S3Folder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "이미지 업로드용 presigned URL 발급 요청")
public record PresignedUrlRequest(

        @Schema(
                description = """
                        업로드 대상 폴더
                        - `PROFILE`: 회원 프로필 이미지
                        - `JOURNAL`: 여행일지 이미지, journalId 필수
                        - `STATIC_PLACE`는 이 API로 발급 대상이 아니므로 넣지 말 것
                        """,
                allowableValues = {"PROFILE", "JOURNAL"},
                example = "PROFILE"
        )
        @NotNull(message = "업로드 대상 폴더는 필수입니다.")
        S3Folder folder,


        @Schema(description = "여행일지 id, folder가 JOURNAL일 때만 필수", example = "10")
        Long journalId,

        @Schema(description = "원본 파일명(확장자 포함)", example = "profile.jpg")
        @NotBlank(message = "파일명은 필수입니다.")
        String fileName
) {
}