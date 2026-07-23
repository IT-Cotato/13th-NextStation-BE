package com.cotato.nextstation.domain.departure.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

// 출발역 추가 응답. 클라이언트가 추가 직전 역 검색에서 이미 역명/노선을 알고 있으므로
// 추가 응답에는 역명/노선을 싣지 않는다(불필요한 조회 방지). 역명/노선은 목록 조회 응답에서 제공한다.
@Schema(description = "출발역 즐겨찾기 추가 결과")
public record DepartureStationCreateResponse(

        @Schema(description = "출발역 즐겨찾기 ID", example = "1")
        Long id,

        @Schema(description = "역 ID", example = "42")
        Long stationId,

        @Schema(description = "표시 순서 (1부터)", example = "1")
        int orderNum,

        @Schema(description = "저장 시각", example = "2026-07-20T12:30:00")
        LocalDateTime createdAt
) {
}
