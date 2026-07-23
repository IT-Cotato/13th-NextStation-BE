package com.cotato.nextstation.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 응답")
public record LoginResponse(

        @Schema(description = "회원 id", example = "1")
        Long memberId,

        @Schema(description = "API 요청 시 Authorization: Bearer 헤더에 담아 보내는 access token. 1시간 후 만료된다.", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken
) {
}