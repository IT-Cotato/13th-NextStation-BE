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

        @Schema(description = "대표 이미지 URL (장소 이미지가 없으면 카테고리 기본 이미지)", example = "https://.../place.jpg")
        String imageUrl,

        @Schema(description = "경도(x)", example = "127.0345")
        Double xCoordinate,

        @Schema(description = "위도(y)", example = "37.5804")
        Double yCoordinate
) {
}
