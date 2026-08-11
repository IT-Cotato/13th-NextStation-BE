package com.cotato.nextstation.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "카카오 로그인/신규가입 판별 요청")
public record KakaoLoginRequest(

        @Schema(description = "카카오 인가코드 (1회용, 발급 후 약 10분 만료)", example = "abcd1234...")
        @NotBlank(message = "인가코드는 필수입니다.")
        String code,

        @Schema(description = "인가코드를 발급받을 때 사용한 redirect_uri. 생략하면 서버 설정의 첫 번째 값을 사용한다.",
                example = "https://next-station-seven.vercel.app/auth/kakao/callback")
        String redirectUri
) {
}