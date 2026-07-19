package com.cotato.nextstation.domain.departure.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "출발역 즐겨찾기")
public record DepartureStationResponse(

        @Schema(description = "출발역 즐겨찾기 ID", example = "1")
        Long id,

        @Schema(description = "역 ID", example = "42")
        Long stationId,

        @Schema(description = "역명 (해당 역을 못 찾으면 null)", example = "왕십리역")
        String stationName,

        @Schema(description = "소속 노선 목록 (환승역이면 여러 개)", example = "[\"2호선\", \"5호선\"]")
        List<String> lines,

        @Schema(description = "표시 순서 (1부터)", example = "1")
        int orderNum,

        @Schema(description = "저장 시각", example = "2026-07-20T12:30:00")
        LocalDateTime createdAt
) {
}
