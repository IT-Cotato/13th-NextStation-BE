package com.cotato.nextstation.domain.recommendation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "뽑힌 역 정보")
public record RecommendedStationResponse(
        @Schema(description = "역 ID", example = "12")
        Long stationId,

        @Schema(description = "역 이름", example = "제기동역")
        String stationName,

        @Schema(description = "역 소개 문구", example = "경동시장과 한방 향기가 가득한 활기찬 전통시장 사이로...")
        String description,

        @Schema(description = "역에서 해볼 만한 일(결과 화면에 역 소개와 함께 노출)", example = "경동시장에서 제철 과일 구경하기")
        String todo,

        @Schema(description = "대표 노선명(환승역이어도 하나만 표시). 뽑기 대상이 아니면 null", example = "1호선")
        String lineName
) {
}
