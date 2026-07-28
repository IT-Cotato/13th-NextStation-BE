package com.cotato.nextstation.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "카카오 로그인/신규가입 판별 요청")
public record KakaoLoginRequest(

        @Schema(description = "카카오 인가코드 (1회용, 발급 후 약 10분 만료)", example = "abcd1234...")
        @NotBlank(message = "인가코드는 필수입니다.")
        String code
) {
}