package com.cotato.nextstation.domain.auth.dto.response;

import com.cotato.nextstation.domain.auth.entity.TermsType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "약관 단건 응답")
public record TermsResponse(

        @Schema(description = "약관 ID", example = "1")
        Long id,

        @Schema(description = "약관 종류", example = "SERVICE")
        TermsType type,

        @Schema(description = "문서 제목. 원문 화면의 헤더에 쓴다. 목록 응답의 동의 항목 이름과 다를 수 있다",
                example = "개인정보처리방침")
        String title,

        @Schema(description = "약관 내용(마크다운 원문)", example = "# 환승여행 서비스 이용약관 ...")
        String content,

        @Schema(description = "약관 버전", example = "v1.0")
        String version,

        @Schema(description = "필수 동의 여부", example = "true")
        boolean isRequired
) {
}