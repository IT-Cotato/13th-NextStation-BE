package com.cotato.nextstation.domain.station.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "코스 만들기 후보 장소")
public record StationPlaceResponse(

        @Schema(description = "장소 ID", example = "101")
        Long placeId,

        @Schema(description = "장소 이름", example = "보문숲길도서관")
        String placeName,

        @Schema(description = "장소 설명", example = "혼자 조용히 머물기 좋은 동네 도서관")
        String description,

        // 폴백(장소 이미지 → 카테고리 기본 이미지)은 장소 조회 쪽에서 이미 적용해 내려준다.
        // 다만 현재는 장소 이미지와 카테고리 기본 이미지가 모두 비어 있어 실제로는 null이 나간다.
        @Schema(description = "대표 이미지 URL. 장소 이미지가 없으면 카테고리 기본 이미지로 대체되며, "
                + "둘 다 없으면 null이다 (이미지 데이터 입수 전까지는 null)",
                example = "https://.../place.jpg", nullable = true)
        String imageUrl,

        @Schema(description = "경도(x)", example = "127.0345")
        Double xCoordinate,

        @Schema(description = "위도(y)", example = "37.5804")
        Double yCoordinate
) {
}
