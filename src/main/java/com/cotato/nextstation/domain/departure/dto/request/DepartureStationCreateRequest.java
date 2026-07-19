package com.cotato.nextstation.domain.departure.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "출발역 즐겨찾기 추가 요청")
public record DepartureStationCreateRequest(

        @Schema(description = "저장할 역 ID (역 검색 API로 얻는다)", example = "42")
        @NotNull(message = "역 ID는 필수입니다.")
        Long stationId,

        @Schema(description = "표시 라벨 (선택, 최대 30자)", example = "집")
        @Size(max = 30, message = "라벨은 30자 이하여야 합니다.")
        String label
) {
}
