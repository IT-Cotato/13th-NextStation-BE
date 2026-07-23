package com.cotato.nextstation.domain.recommendation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "코스 미리보기 장소(카테고리별 1개)")
public record CoursePreviewPlaceResponse(
        @Schema(description = "장소 ID", example = "101")
        Long placeId,

        @Schema(description = "장소 이름", example = "경동시장")
        String placeName,

        @Schema(description = "장소 설명", example = "서울 최대 규모의 전통시장")
        String description,

        @Schema(description = "카테고리 코드", example = "CULTURE", allowableValues = {"CAFE", "FOOD", "CULTURE", "WALK"})
        String categoryCode,

        @Schema(description = "카테고리 표시명", example = "문화공간")
        String categoryName,

        @Schema(description = "대표 이미지 URL(장소 이미지 없으면 카테고리 기본 이미지)", example = "https://.../place.jpg")
        String imageUrl,

        @Schema(description = "경도(x)", example = "127.0345")
        Double xCoordinate,

        @Schema(description = "위도(y)", example = "37.5804")
        Double yCoordinate
) {
}
