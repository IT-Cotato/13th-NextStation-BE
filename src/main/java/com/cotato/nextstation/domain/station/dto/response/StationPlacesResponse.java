package com.cotato.nextstation.domain.station.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "역별 장소 목록 조회 응답 (코스 만들기 후보)")
public record StationPlacesResponse(

        @Schema(description = "역 ID", example = "6")
        Long stationId,

        @Schema(description = "역 이름", example = "보문역")
        String stationName,

        @Schema(description = "코스 저장 시 기본으로 채워줄 이름 (사용자가 수정 가능)", example = "보문역 환승여행 코스")
        String defaultCourseName,

        @Schema(description = "카테고리별 장소 목록 (문화공간 → 식당 → 카페 → 산책포인트 순, 장소 없는 카테고리는 제외)")
        List<StationPlaceCategoryResponse> categories
) {
}
