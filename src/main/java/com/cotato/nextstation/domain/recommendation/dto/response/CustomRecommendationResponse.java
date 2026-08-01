package com.cotato.nextstation.domain.recommendation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "맞춤추천 결과")
public record CustomRecommendationResponse(
        @Schema(description = "추천된 역 정보")
        RecommendedStationResponse station,

        @Schema(description = "출발역 기준 소요시간(분)", example = "24")
        int travelDurationMinutes
) {
}
