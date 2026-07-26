package com.cotato.nextstation.domain.auth.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// 카카오 API 4xx 응답 본문 매핑용
// error_description은 인가코드 등 민감정보를 그대로 echo하는 경우가 있어 로그에 남기지 않는다
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoErrorResponse(
        String error,
        @JsonProperty("error_code") String errorCode,
        @JsonProperty("error_description") String errorDescription
) {
}