package com.cotato.nextstation.domain.auth.dto.response;

import com.cotato.nextstation.domain.auth.entity.TermsType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "약관 목록 응답. 원문(content)은 단건 조회에서 내려간다")
public record TermsSummaryResponse(

        @Schema(description = "약관 ID. 회원가입 동의 요청(agreedTermsIds)에 쓴다", example = "1")
        Long id,

        @Schema(description = "약관 종류. 단건 조회 경로에 쓴다. 시더가 만들지 않은 약관은 null", example = "SERVICE")
        TermsType type,

        @Schema(description = "동의 항목 이름. 동의 화면 목록에 쓴다. 원문 화면의 문서 제목과 다를 수 있다",
                example = "개인정보 수집 및 이용 동의")
        String title,

        @Schema(description = "약관 버전", example = "v1.0")
        String version,

        @Schema(description = "필수 동의 여부", example = "true")
        boolean isRequired
) {
}