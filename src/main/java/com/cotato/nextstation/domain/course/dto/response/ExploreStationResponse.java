package com.cotato.nextstation.domain.course.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

// 노선따라 둘러보기의 "역 선택" 드롭다운 항목.
// 이름만 주면 프론트가 필터 요청에 쓸 id를 알 수 없어 함께 내려준다.
@Schema(description = "역 필터 항목")
public record ExploreStationResponse(

        @Schema(description = "역 ID. 그대로 stationId 파라미터에 넣어 요청한다", example = "123")
        Long stationId,

        @Schema(description = "역 이름", example = "보문역")
        String stationName
) {
}
