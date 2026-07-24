package com.cotato.nextstation.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "액세스 토큰 재발급 응답")
public record ReissueResponse(

        @Schema(description = "새로 발급된 access token. 1시간 후 만료된다.", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken
) {
}
