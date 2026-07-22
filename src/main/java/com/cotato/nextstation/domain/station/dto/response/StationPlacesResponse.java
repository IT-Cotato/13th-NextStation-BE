package com.cotato.nextstation.domain.station.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "역별 장소 목록 조회 응답 (코스 만들기 후보)")
public record StationPlacesResponse(

        @Schema(description = "역 ID", example = "6")
        Long stationId,

        @Schema(description = "역 이름", example = "보문역")
        String stationName,

        @Schema(description = "역 소개 문구", example = "성북천을 따라 천천히 걷고, 대학가와 오래된 주거 골목 사이의 조용한 생활감을 느낄 수 있는 역이에요.")
        String description,

        @Schema(description = "대표 노선명(화면 배지에 표시). 뽑기 대상이 아니면 null", example = "6호선")
        String lineName,

        @Schema(description = "소속 노선 전부(환승역이면 여러 개). 대표 노선도 포함된다", example = "[\"6호선\", \"우이신설선\"]")
        List<String> lines,

        @Schema(description = "역 대표 태그(역 내 장소들의 태그 상위 3개)", example = "[\"LOCAL_EXPLORE\", \"NATURE\", \"BUDGET\"]")
        List<String> tags,

        @Schema(description = "코스 저장 시 기본으로 채워줄 이름 (사용자가 수정 가능)", example = "보문역 환승여행 코스")
        String defaultCourseName,

        @Schema(description = "카테고리별 장소 목록 (문화공간 → 식당 → 카페 → 산책포인트 순, 장소 없는 카테고리는 제외)")
        List<StationPlaceCategoryResponse> categories
) {
}
