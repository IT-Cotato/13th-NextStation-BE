package com.cotato.nextstation.domain.place.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "역별/태그별 장소 수 집계 (맞춤추천 알고리즘용)")
public record StationTagCountResponse(
        @Schema(description = "stationId → { tagName → 장소 수 } 형태의 집계 결과")
        Map<Long, Map<String, Long>> counts
) {
}