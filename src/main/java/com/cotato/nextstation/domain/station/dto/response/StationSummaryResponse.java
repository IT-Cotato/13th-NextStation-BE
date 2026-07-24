package com.cotato.nextstation.domain.station.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "역 요약 (검색 결과 / 출발역 표시용)")
public record StationSummaryResponse(

        @Schema(description = "역 ID", example = "42")
        Long stationId,

        @Schema(description = "역명", example = "왕십리역")
        String stationName,

        @Schema(description = "소속 노선 목록 (환승역이면 여러 개). 노선 표시 순서는 노선 ID 순이다.")
        List<LineSummaryResponse> lines
) {
}
