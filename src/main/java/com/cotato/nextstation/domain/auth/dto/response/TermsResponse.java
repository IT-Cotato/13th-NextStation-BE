package com.cotato.nextstation.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "약관 응답")
public record TermsResponse(

        @Schema(description = "약관 ID", example = "1")
        Long id,

        @Schema(description = "약관 제목", example = "서비스 이용약관")
        String title,

        @Schema(description = "약관 내용", example = "제1조 (목적) ...")
        String content,

        @Schema(description = "약관 버전", example = "v1.0")
        String version,

        @Schema(description = "필수 동의 여부", example = "true")
        boolean isRequired
) {
}