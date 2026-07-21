package com.cotato.nextstation.domain.recommendation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "랜덤뽑기 결과(뽑힌 역 + 코스 미리보기)")
public record RandomRecommendationResponse(
        @Schema(description = "뽑힌 역 정보")
        RecommendedStationResponse station,

        @Schema(description = "역 기준 코스 미리보기")
        CoursePreviewResponse course
) {
}
